package com.boubei.tss.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jxl.Cell;
import jxl.CellType;
import jxl.DateCell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

import org.apache.log4j.Logger;

import com.boubei.tss.dm.DataExport;

public class ExcelUtils {
	
	static Logger log = Logger.getLogger(ExcelUtils.class);
	
	public static String CSV_FIX = ".csv";
	public static String XLS_FIX = ".xls";
	
	public static boolean isCSV(String filepath) {
		return filepath.toLowerCase().endsWith( ExcelUtils.CSV_FIX );
	}
	
	public static boolean isXLS(String filepath) {
		return filepath.toLowerCase().endsWith( ExcelUtils.XLS_FIX );
	}
	
	public static String csv2Excel(String sourceFile) {
		return csv2Excel(sourceFile, DataExport.CSV_GBK);
	}
	
	public static String csv2Excel(String sourceFile, String charSet) {
		String sourceFileName = new File(sourceFile).getName();
		String targetFileName = FileHelper.getFileNameNoSuffix( sourceFileName ) + XLS_FIX;
		
		return csv2Excel(sourceFile, targetFileName, charSet);
	}
	
	public static String csv2Excel(String sourceFile, String targetFileName, String charSet) {
		File csvFile = new File(sourceFile);
		
		String targetFile = DataExport.getExportPath() + "/" + targetFileName;
		WritableWorkbook wwb = null;
		FileOutputStream ios = null;
		try {
			wwb = Workbook.createWorkbook( ios = new FileOutputStream(targetFile) );
			WritableSheet ws = wwb.createSheet( FileHelper.getFileNameNoSuffix(csvFile.getName()), 0 );
			
			String dataStr = FileHelper.readFile(csvFile, charSet);
			String[] rows = EasyUtils.split(dataStr, "\n");
			
			for (int i = 0; i < rows.length; i++) {
				String[] row = rows[i].split(",");
				for (int j = 0; j < row.length; j++) {
					ws.addCell( new jxl.write.Label(j, i, row[j]) );
				}
			}
			wwb.write();
		} 
		catch (Exception e) {
			log.error(e.getMessage(), e.getCause());
		} 
		finally {
			try { wwb.close(); } catch (Exception e) {}
			try { ios.close(); } catch (Exception e) {}
		}
		
		return targetFile;
	}
 
	public static String excel2CSV(String sourceFile) {
		return excel2CSV(sourceFile, DataExport.CSV_GBK);
	}
	@SuppressWarnings("unchecked")
	public static String excel2CSV(String sourceFile, String charSet) {
		String sourceFileName = new File(sourceFile).getName();
		String targetFileName = FileHelper.getFileNameNoSuffix( sourceFileName ) + CSV_FIX;
		
		Map<String, Object> result = readExcel(sourceFile);
		List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
		List<String> headers = (List<String>) result.get("cnFields");
		return DataExport.exportCSV(targetFileName, data, headers, charSet);
	}
	
	/**
	 * 读取excel文件数据第一个sheet数据
	 * 
	 * @param filepath
	 * @return {headers: list, data: list}
	 */
	private static Map<String, Object> readExcel(String filepath) {
		List<String> headers = new ArrayList<String>();
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
		
		InputStream is = null;
		Workbook rwb = null;
		try {
			is = new FileInputStream(filepath);
			rwb = Workbook.getWorkbook(is);
			
			Sheet sheet1 = rwb.getSheet(0);       // 获取第一张Sheet表
			int rsColumns = sheet1.getColumns();  // 获取Sheet表中所包含的总列数
			int rsRows = sheet1.getRows();        // 获取Sheet表中所包含的总行数
			
			// 获取指定单元格的对象引用
			for (int i = 0; i < rsRows; i++) {
				Map<String, Object> row = new LinkedHashMap<String, Object>();
				
				for (int j = 0; j < rsColumns; j++) {
					Cell cell = sheet1.getCell(j, i);
					
					String value = null;
					if (cell.getType() == CellType.DATE) { // 日期格式处理方式
						DateCell dc = (DateCell) cell;
						Date date = dc.getDate();
						date = DateUtil.subDays(date, (double) 1 / 3);
						value = DateUtil.format(date, "yyyy-MM-dd HH:mm:ss");
					} else {
						value = cell.getContents();
					}
					
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
			log.error(e.getMessage(), e.getCause());
		} 
		finally {
			try { is.close();  } catch (Exception e) {}
			try { rwb.close(); } catch (Exception e) {}
		}
		
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("data", data);
		result.put("cnFields", headers);
		return result;
	}

}
