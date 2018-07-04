package com.boubei.tss.dm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;

/**
 * 1.org.apache.poi.ss.usermodel.Workbook 对应Excel文档；
　　2.org.apache.poi.hssf.usermodel.HSSFWorkbook  对应xls格式的Excel文档；
　　3.org.apache.poi.xssf.usermodel.XSSFWorkbook  对应xlsx格式的Excel文档；
　　4.org.apache.poi.ss.usermodel.Sheet  对应Excel文档中的一个sheet；
　　5.org.apache.poi.ss.usermodel.Row    对应一个sheet中的一行；
　　6.org.apache.poi.ss.usermodel.Cell   对应一个单元格。
 *
 */
public class ExcelPOI extends Excel {
	
	protected  String _csv2Excel(String sourceFile, String charSet) {
		File csvFile = new File(sourceFile);
		String csvName = FileHelper.getFileNameNoSuffix( csvFile.getName() );
		
		String targetFile = DataExport.getExportPath() + "/" + csvName + XLSX_FIX; // POI 一律输出 XLSX;
		Workbook wb = null;
		FileOutputStream ios = null;
		try {
			wb = new XSSFWorkbook();
			Sheet ws = wb.createSheet( csvName );
			
			String dataStr = FileHelper.readFile(csvFile, charSet);
			String[] rows = EasyUtils.split(dataStr, "\n");
			
			for (int i = 0; i < rows.length; i++) {
				String[] rowData = rows[i].split(",");
				Row row = ws.createRow(i);
				
				for (int j = 0; j < rowData.length; j++) {
					Cell cell = row.createCell(j, CellType.STRING);
					cell.setCellValue( rowData[j] );
				}
			}
			wb.write(  ios = new FileOutputStream(targetFile)  );
		} 
		catch (Exception e) {
			log.error(e.getMessage(), e.getCause());
		} 
		finally {
			try { wb.close(); } catch (Exception e) {}
			try { ios.close(); } catch (Exception e) {}
		}
		
		return targetFile;
	}

	protected Map<String, Object> readExcel(String filepath) {
		List<String> headers = new ArrayList<String>();
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
		
		InputStream is = null;
		Workbook wb = null;
		try {
			is = new FileInputStream(filepath);
			wb = isXLS(filepath) ? new HSSFWorkbook(is) : new XSSFWorkbook(is);
			
			Sheet sheet1 = wb.getSheetAt(0);   // 获取第一张Sheet表
			Row row0 = sheet1.getRow(0);      // 获取第一行
            
			int rsColumns = row0.getPhysicalNumberOfCells();  // 获取Sheet表中所包含的总列数
			int rsRows  = sheet1.getPhysicalNumberOfRows();   // 获取Sheet表中所包含的总行数
			
			// 获取指定单元格的对象引用
			for (int i = 0; i < rsRows; i++) {
				Map<String, Object> row = new LinkedHashMap<String, Object>();
				Row _row = sheet1.getRow(i);
				
				for (int j = 0; j < rsColumns; j++) {
					Cell cell = _row.getCell(j);
					String value = getCellVal(cell, i, j);
					
					if (i == 0) {
						headers.add(value);
					}
					else {
						row.put(headers.get(j), value);
					}
				}
				
				if (i > 0) data.add(row);
			}
		} 
		catch (Exception e) {
			log.error( "readExcel error: " + e.getMessage(), e.getCause() );
		} 
		finally {
			try { is.close(); } catch (Exception e) {}
			try { wb.close(); } catch (Exception e) {}
		}
		
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("data", data);
		result.put("headers", headers);
		return result;
	}

	static String getCellVal(Cell cell, int i, int j) {
		if(cell == null) return "";
		
		try {
			//判断cell类型
	        switch(cell.getCellTypeEnum()) {
		        case NUMERIC:
		            // 判断cell是否为日期格式
		            if(org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)){
		                return DateUtil.format( cell.getDateCellValue() );
		            } 
		            else { // 数字
		            	return String.valueOf(cell.getNumericCellValue());
		            }
		        case FORMULA:
		        	return ( (XSSFCell)cell ).getCTCell().getV();
		        case STRING:
		        	return cell.getStringCellValue();
		        default:
		        	return cell.toString();
	        }
		} 
		catch( Exception e ) {
			throw new BusinessException( "Excel.getCellVal error, location = [" + i + "," + j + "], cell = " + cell, e);
		}
	}
}
