package com.boubei.tss.dm.record.file;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.boubei.tss.EX;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.ddl._Database;
import com.boubei.tss.dm.ddl._Field;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.util.EasyUtils;

/**
 * TSS默认数据校验器
 */
public class DefaultDataVaild implements IDataVaild {

	public void vaild(_Database _db, List<List<String>> rows, List<String> headers, String uniqueCodes, List<String> valSQLFields,
			List<String> errLines, List<Integer> errLineIndexs) {
		
		Map<String, List<Object>> colValues = new HashMap<String, List<Object>>();
		
		List<String> labels = new ArrayList<String>();
		labels.addAll( headers );
		labels.addAll(valSQLFields);
		rows.set( 0, labels );
		
		for(int index = 1; index < rows.size(); index++) { // 第一行为表头，不要
			
			List<String> fieldVals = rows.get(index);
			
			// 0、检查列数是否和表头列数相等
			if( fieldVals.size() != headers.size() ) {
				String err;
				if(fieldVals.size() > headers.size()) {
					err = "行数据列数大于表头列数量";
				} else {
					err = EX.DM_23;
				}
				errLines.add( index + "," + err.replaceAll(",", "，") + "," + EasyUtils.list2Str(fieldVals) );
				errLineIndexs.add(index);
				continue;
			}
			
			List<String> errors = new ArrayList<String>();
			Map<String, Object> valuesMap = new LinkedHashMap<String, Object>();
			
			for(int j = 0; j < labels.size(); j++) {
    			String filedLabel = labels.get(j);
    			String fieldCode = _db.ncm.get(filedLabel);
    			if(fieldCode == null) {
    				errors.add("列【" + filedLabel + "】在【" +_db.recordName+ "】定义里不存在;");
    			}
    			
    			String value = (j >= fieldVals.size()) ? "" : fieldVals.get(j);
    			
    			// 1、根据【默认值】补齐字段信息：关联字段值获取等
    			String valSQL = _db.csql.get(fieldCode);
    			if( EasyUtils.isNullOrEmpty(value) && !EasyUtils.isNullOrEmpty(valSQL) ) {
    				// 自动关联字段值（eg：根据客户 + 位置信息获取位置的编码值，条件列需要在被补列前面）
    	    		String _sql = DMUtil.fmParse(valSQL.replaceAll("\\^", "'"), valuesMap);
    	    		List<Map<String, Object>> result = SQLExcutor.query(_db.datasource, _sql);
    	    		if( result.size() > 1 ) {
    	    			errors.add(filedLabel + "匹配到多个数据");
    	    		}
    	    		else if( result.size() == 0 ) {
    	    			errors.add(filedLabel + "没有匹配的数据");
    	    		} else {
    	    			value = EasyUtils.obj2String(result.get(0).get(fieldCode));
    	    		}
    			}
    			
    			// 1、nullable、unique、type 校验
    			boolean nullable = "false".equals(_db.cnull.get(fieldCode));
    			boolean unique 	= "true".equals(_db.cuni.get(fieldCode));
    			String type	= _db.ctype.get(fieldCode);
    			
    			if( type != null) {
	    			try {
	    				value = EasyUtils.obj2String(  DMUtil.preTreatValue(value, type) );
	    			} catch(Exception e) {
	    				errors.add(filedLabel + "数据类型【" +type+ "】异常:" + e.getMessage());
	    			}
    			}
    			
    			String defaultVal = _db.cval.get(fieldCode);
    			if(nullable && EasyUtils.isNullOrEmpty(value) && !_Field.isAutoSN(defaultVal)) {
    				errors.add(filedLabel + "值不能为空");
    			}
    			if(unique && !EasyUtils.isNullOrEmpty(value)) {
    				List<Object> colList = colValues.get(fieldCode + "_csv");
    				if(colList == null) {
    					colValues.put(fieldCode + "_csv", colList = new ArrayList<Object>());
    				}
    				int rowIndex = colList.indexOf(value);
					if( rowIndex >= 0) {
    					errors.add(filedLabel + "值不唯一，和第" +(rowIndex+1)+ "行数据重复");
    				} else {
    					colList.add(value);
    					
    					// 检查是否和数据库里既有数据重复（如果uniqueCodes 为配置了uinique限制的字段，不做数据库数据检查，会自动按uniqueCodes覆盖）
    					if( !fieldCode.equalsIgnoreCase(uniqueCodes) ) {
    						Map<String, String> params = new HashMap<String, String>();
        					params.put(fieldCode, value);
    						if( _db.select(1, 1, params).count > 0) {
    							errors.add(filedLabel + "值不唯一，在数据库里已存在");
    						}
    					}
    				}
    			}
    			
    			// 2、正则表达式校验
    			String checkReg = _db.creg.get(fieldCode);
    			if( !EasyUtils.isNullOrEmpty(checkReg) && !EasyUtils.isNullOrEmpty(value) ) {
    				String regExp = checkReg.replaceAll("\\\\","\\\\\\\\");  // JS 正则转换为 JAVA正则
        	        Pattern p = Pattern.compile(regExp);  
        	        if( !p.matcher(value).matches() ) {
        	        	String errorMsg = _db.cerr.get(fieldCode);
        	        	errors.add( (String) EasyUtils.checkNull(errorMsg, filedLabel + "值校验异常") );
        	        }
    			}
    			
    			valuesMap.put(fieldCode, value);
        	}
			
			if( errors.size() > 0 ) {
				String errLine = index + "," + EasyUtils.list2Str(errors, "|").replaceAll(",", "，") + "," + EasyUtils.list2Str(fieldVals);
				
				errLines.add(errLine);
				errLineIndexs.add(index);
				continue;
			}
			
			String valueStr = EasyUtils.list2Str( valuesMap.values() );
			rows.set(index, EasyUtils.toList( valueStr ) );
		}
	}
	
}
