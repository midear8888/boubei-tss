package com.boubei.tss.dm;

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

import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;

public class ExcelJXL extends Excel {
 
	protected String _csv2Excel(String sourceFile, String charSet) {
		File csvFile = new File(sourceFile);
		String csvName = FileHelper.getFileNameNoSuffix( csvFile.getName() );
		
		String targetFile = DataExport.getExportPath() + "/" + csvName + XLS_FIX; // JXL 一律输出 XLS;
		
		WritableWorkbook wwb = null;
		FileOutputStream ios = null;
		try {
			wwb = Workbook.createWorkbook( ios = new FileOutputStream(targetFile) );
			WritableSheet ws = wwb.createSheet( csvName, 0 );
			
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
 
	protected Map<String, Object> readExcel(String filepath) {
		List<String> headers = new ArrayList<String>();
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
		
		InputStream is = null;
		Workbook wb = null;
		try {
			is = new FileInputStream(filepath);
			wb = Workbook.getWorkbook(is);
			
			Sheet sheet1 = wb.getSheet(0);       // 获取第一张Sheet表
			int rsColumns = sheet1.getColumns();  // 获取Sheet表中所包含的总列数
			int rsRows = sheet1.getRows();        // 获取Sheet表中所包含的总行数
			
			// 获取指定单元格的对象引用
			for (int i = 0; i < rsRows; i++) {
				Map<String, Object> row = new LinkedHashMap<String, Object>();
				
				for (int j = 0; j < rsColumns; j++) {
					Cell cell = sheet1.getCell(j, i);
					String value = getCellVal(cell);
					
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
			try { wb.close(); } catch (Exception e) {}
		}
		
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("data", data);
		result.put("headers", headers);
		return result;
	}
	
	private static String getCellVal(Cell cell) {
		String value = null;
		if (cell.getType() == CellType.DATE) { // 日期格式处理方式
			DateCell dc = (DateCell) cell;
			Date date = dc.getDate();
			date = DateUtil.subDays(date, (double) 1 / 3);
			value = DateUtil.format(date, "yyyy-MM-dd HH:mm:ss");
		} else {
			value = cell.getContents();
		}
		
		return value;
	}

}
