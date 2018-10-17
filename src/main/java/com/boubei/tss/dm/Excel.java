/* ==================================================================   
 * Created [2018-06-22] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.boubei.tss.util.FileHelper;

public abstract class Excel {
	
	static Logger log = Logger.getLogger(Excel.class);
	
	public static Excel instance = new ExcelPOI();
	
	public static String CSV_FIX = ".csv";
	public static String XLS_FIX = ".xls";
	public static String XLSX_FIX = ".xlsx";
	
	public static boolean isCSV(String filepath) {
		return filepath.toLowerCase().endsWith( CSV_FIX );
	}
	public static boolean isXLS(String filepath) {
		return filepath.toLowerCase().endsWith( XLS_FIX );
	}
	public static boolean isXLSX(String filepath) {
		return filepath.toLowerCase().endsWith( XLSX_FIX );
	}
	
	
	public static String csv2Excel(String sourceFile) {
		return csv2Excel(sourceFile, DataExport.CSV_GBK);
	}
	
	public static String csv2Excel(String sourceFile, String charSet) {
		return instance._csv2Excel(sourceFile, charSet);
	}
	
	public static String excel2CSV(String sourceFile) {
		return excel2CSV(sourceFile, DataExport.CSV_GBK);
	}
	
	@SuppressWarnings("unchecked")
	public static String excel2CSV(String sourceFile, String charSet) {
		String sourceFileName = new File(sourceFile).getName();
		String targetFileName = FileHelper.getFileNameNoSuffix( sourceFileName ) + CSV_FIX;
		
		Map<String, Object> result = instance.readExcel(sourceFile);
		List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
		List<String> headers = (List<String>) result.get("headers");
		return DataExport.exportCSV(targetFileName, data, headers, charSet);
	}
	
	/**
	 * 把CSV文件转成Excel文件
	 */
	protected abstract String _csv2Excel(String sourceFile, String charSet);
	
	/**
	 * 读取excel文件数据第一个sheet数据
	 * 
	 * @param filepath
	 * @return {headers: list, data: list}
	 */
	protected abstract Map<String, Object> readExcel(String sourceFile);

}
