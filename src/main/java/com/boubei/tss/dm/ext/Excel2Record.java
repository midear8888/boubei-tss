package com.boubei.tss.dm.ext;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.Excel;
import com.boubei.tss.dm.ExcelPOI;
import com.boubei.tss.dm.ddl._Database;
import com.boubei.tss.dm.ddl._Field;
import com.boubei.tss.dm.record.Record;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.web.servlet.AfterUpload;
import com.boubei.tss.util.EasyUtils;

/**
 * 直接依据Excel sheet页创建录入表，可多个sheet 页一次性创建；
 */
public class Excel2Record implements AfterUpload {

	Logger log = Logger.getLogger(this.getClass());
	
	RecordService recordService = (RecordService) Global.getBean("RecordService");
	String dataSource;
	
	public String processUploadFile(HttpServletRequest request, String filepath, String orignFileName) throws Exception {
		
		Long groupId = EasyUtils.obj2Long( request.getParameter("groupId") );
		dataSource = request.getParameter("dataSource");
		List<Record> result = new ArrayList<Record>();
		
		if( Excel.isCSV(filepath) ) {
			filepath = Excel.csv2Excel(filepath);
		}
 
		InputStream is = null;
		Workbook wb = null;
		try {
			is = new FileInputStream(filepath);
			boolean isXLS = Excel.isXLS(filepath);
			ExcelPOI.checkExcelSize(is, isXLS);
			
			wb = isXLS ? new HSSFWorkbook(is) : new XSSFWorkbook(is);
			
			Iterator<Sheet> sheets = wb.iterator();
			while(sheets.hasNext()) {
				sheet2Record(groupId, sheets.next(), result);
			}
		} 
		catch (Exception e) {
			throw new BusinessException( "readExcel error: " + e.getMessage(), e);
		} 
		finally {
			try { is.close(); } catch (Exception e) {}
			try { wb.close(); } catch (Exception e) {}
		}
		
		return "parent.alert('成功导入" +result.size()+ "个Sheet页到数据表.'); parent.loadInitData();";
	}
	
	public void sheet2Record(Long groupId, Sheet sheet, List<Record> records) {
		List<String> headers = new ArrayList<String>();
		List<String> types = new ArrayList<String>();
		List<List<String>> rows = new ArrayList<List<String>>();
		
		String sheetName = sheet.getSheetName();
		Row row0 = sheet.getRow(0);      // 获取第一行
		if(row0 == null) return;
		
		int rsColumns = row0.getPhysicalNumberOfCells();  // 获取Sheet表中所包含的总列数
		int rsRows  = sheet.getPhysicalNumberOfRows();   // 获取Sheet表中所包含的总行数
		
		// 获取指定单元格的对象引用
		for (int i = 0; i < rsRows; i++) {
			Row _row = sheet.getRow(i);
			List<String> row = new ArrayList<String>();
			
			for (int j = 0; j < rsColumns; j++) {
				Cell cell = _row.getCell(j);
				if (i == 0) {
					String value = cell.getStringCellValue();
					if( !EasyUtils.isNullOrEmpty(value) ) {
						headers.add(value);
					}
					types.add( getCellType(cell) );
				}
				else {
					if (i == 1) {
						types.set(j, getCellType(cell) );
					}
					row.add( ExcelPOI.getCellVal(cell, i, j) );
				}
			}
			
			if( EasyUtils.list2Str(row, "").trim().length() > 0 ) { // 忽略空行
				rows.add( row );
			}
		}
		
		// 创建数据表
		if(headers.size() != types.size()) return; // 忽略带符合表头的sheet页
		
		Record record = new Record();
        record.setType( Record.TYPE1 );
        record.setParentId( groupId );
		record.setName( sheetName );
        record.setDatasource( (String) EasyUtils.checkNull(dataSource, DMConstants.LOCAL_CONN_POOL) );
        record.setTable( Record.DEFAULT_TABLE );
        String define = "[";
        for(int i = 0; i < headers.size(); i++) {
        	define += "{'label':'" +headers.get(i)+ "', 'code':'c" +(i+1)+ "', 'type':'" +types.get(i)+ "'}";
        	if(i != headers.size() - 1) {
        		define += ",";
        	}
        }
        define += "]";
        record.setDefine(define);
        record = recordService.createRecord(record);
		
		records.add(record);
		
		// 导入sheet页里数据到record
		_Database _db = recordService.getDB( record.getId() );
    	
		List<Map<String, String>> insertList = new ArrayList<Map<String, String>>();
		
		for(List<String> row : rows) { 
			Map<String, String> item = new HashMap<String, String>();
			for(int j = 0; j < row.size(); j++) {
    			item.put( _db.ncm.get(headers.get(j)), row.get(j));
        	}
			
			insertList.add(item);
		}
    	_db.insertBatch(insertList); 
	}

	static String getCellType(Cell cell) {
		switch(cell.getCellTypeEnum()) {
	        case NUMERIC:
	            if( org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell) ) { // 判断cell是否为日期格式
	            	return _Field.TYPE_DATETIME;
	            } 
	            else { // 数字
	            	return _Field.TYPE_NUMBER;
	            }
	        case FORMULA:
	        	return _Field.TYPE_STRING;
	        case STRING:
	        default:
	        	return _Field.TYPE_STRING;
	    }
	}
}
