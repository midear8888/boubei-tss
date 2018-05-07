/* ==================================================================   
 * Created [2016-3-5] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.record.file;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.boubei.tss.EX;
import com.boubei.tss.dm.DataExport;
import com.boubei.tss.dm.ddl._Database;
import com.boubei.tss.dm.record.Record;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.web.servlet.AfterUpload;
import com.boubei.tss.modules.sn.SerialNOer;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;

/**
 * var url = URL_UPLOAD_FILE + "?afterUploadClass=com.boubei.tss.dm.record.file.ImportCSV";
   url += "&recordId=" + recordId;
   url += "&uniqueCodes=oto,phone";
    
 * 根据数据表提供的导入模板，填写后导入实现批量录入数据。
 * CSV文件需满足条件：
 * 1、模板从 xdata/import/tl/ 接口导出
 * 2、表头列的字段名需要 和 录入表定义的字段Label严格一致，且不能重复
 * 3、每一行数据的列数及顺序 == 表头的列数及顺序，不能多也不能少
 * 4、每个字段值不允许存在 换行符、英文逗号
 * 5、覆盖式导入，需定义判断规则，支持多个字段（ 定义在数据表页面【全局脚本】里：var uniqueCodes="oto,sjphone";）
 * 
 * TODO 批量插入，如果某一批（目前10000一批）出错，如何回滚所有已经插入的数据
 */
public class ImportCSV implements AfterUpload {

	Logger log = Logger.getLogger(this.getClass());
	
	RecordService recordService = (RecordService) Global.getBean("RecordService");

	public String processUploadFile(HttpServletRequest request,
			String filepath, String oldfileName) throws Exception {

		Long recordId  = Long.parseLong(request.getParameter("recordId"));
		Record record = recordService.getRecord(recordId);
		_Database _db = _Database.getDB(record);
		int insertCount = 0, updateCount = 0;
		List<Integer> ignoreLines = new ArrayList<Integer>();
		
		boolean ignoreExist = "true".equals( request.getParameter("ignoreExist") );
		String uniqueCodes = request.getParameter("uniqueCodes");
		String charSet = (String) EasyUtils.checkNull(request.getParameter("charSet"), DataExport.CSV_GBK); // 默认GBK

		// 解析附件数据
		File targetFile = new File(filepath);
		String dataStr = FileHelper.readFile(targetFile, charSet); 
		dataStr = dataStr.replaceAll(";", ","); // mac os 下excel另存为csv是用分号;分隔的
		String[] rows = EasyUtils.split(dataStr, "\n");
		if(rows.length < 2) {
			return "parent.alert('导入文件没有数据');";
		}
		
		String[] headers = rows[0].split(",");
		int messyCount = 0;
		for(String fieldName : headers) {
			if( !_db.ncm.containsKey(fieldName) ) messyCount++; // 表头名 在数据表字段定义里不存在
		}
		if( messyCount*1.0 / headers.length > 0.5 ) { // header一半以上都找不着，可能是CSV文件为UTF-8编码，以UTF-8再次尝试盗取
			dataStr = FileHelper.readFile(targetFile, DataExport.CSV_UTF8); 
			dataStr = dataStr.replaceAll(";", ",");
			rows = EasyUtils.split(dataStr, "\n");
			headers = rows[0].split(",");
		}
		
		List<String> snList = null;
		List<Map<String, String>> valuesMaps = new ArrayList<Map<String, String>>();
		
		for(int index = 1; index < rows.length; index++) { // 第一行为表头，不要
			String row = rows[index];
			String[] fieldVals = (row+ " ").split(",");
			
			if(fieldVals.length != headers.length) {
				throw new BusinessException(EX.parse(EX.DM_23, index));
			}
			
			Map<String, String> valuesMap = new HashMap<String, String>();
			String sb = "";
			for(int j = 0; j < fieldVals.length; j++) {
    			String value = fieldVals[j].trim();
    			value = value.replaceAll("，", ","); // 导出时英文逗号替换成了中文逗号，导入时替换回来
    			sb += value;
    			
    			String filedLabel = headers[j];
    			String fieldCode = _db.ncm.get(filedLabel); //_db.fieldCodes.get(j);
    			
    			// 检查值为空的字段，是否配置自动取号规则，是的话先批量取出一串连号
    			String defaultVal = _db.cvm.get(fieldCode);  //fieldValues.get(j);
    			if( EasyUtils.isNullOrEmpty(value) && (defaultVal+"").endsWith("yyMMddxxxx")) {
    				String preCode = defaultVal.replaceAll("yyMMddxxxx", "");
    				if(snList == null) {
    					snList = new SerialNOer().create(preCode, rows.length);
    				}
    				value = snList.get(index - 1);
    			}
    			
				valuesMap.put(fieldCode, value);
        	}
			if( EasyUtils.isNullOrEmpty(sb) ) { // 判断是否每个字段都没有数据，是的话为空行
				continue;
			}
			
			// 支持覆盖式导入，覆盖规则为参数指定的某个（或几个）字段
			if( !EasyUtils.isNullOrEmpty(uniqueCodes) ) {
				// 检测记录是否已经存在
				Map<String, String> params = new HashMap<String, String>();
				String[] codes = uniqueCodes.trim().split(",");
				boolean hasNullParam = false;
				for(String code : codes) {
					code = code.trim();
					String value = valuesMap.get(code); // 值不能为空，为空会查出无辜的数据覆盖
					if( EasyUtils.isNullOrEmpty(value) ) {
						hasNullParam = true;
					}
					params.put(code, value);
				}
				if( !hasNullParam ) {
					List<Map<String, Object>> result = _db.select(1, 1, params).result;
					if( result.size() > 0 ) {
						if( ignoreExist ) {
							ignoreLines.add(index);
						} else {
							Map<String, Object> old = result.get(0);
							Long itemId = EasyUtils.obj2Long(old.get("id"));
							_db.update(itemId, valuesMap);
							
							updateCount ++;
						}
						
						continue;
					}
				}
			}
			
			// 批量新增
			valuesMaps.add(valuesMap);
			insertCount ++;
			
			if(valuesMaps.size() == 10000) { // 按每一万批量插入一次
				_db.insertBatch(valuesMaps);
				valuesMaps.clear();
			}
		}
    	_db.insertBatch(valuesMaps);
		
		// 向前台返回成功信息
    	String noInserts = ignoreExist ? ("忽略了第【" +EasyUtils.list2Str(ignoreLines)+ "】行") : ("覆盖" +updateCount+ "行");
		return "parent.alert('导入完成：共新增" +insertCount+ "行，" +noInserts+ "。请刷新查看。'); parent.openActiveTreeNode();";
	}
	
}