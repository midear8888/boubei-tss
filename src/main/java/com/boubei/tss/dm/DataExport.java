/* ==================================================================   
 * Created [2016-3-5] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.modules.param.ParamConfig;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.StringUtil;

public class DataExport {
	
	public static final String CHARSET = "charSet";
	public static final String CSV_GBK = "GBK";
	public static final String CSV_UTF8 = "UTF-8";
	public static final String SYS_CHAR_SET = ParamConfig.getAttribute("SYS_CHAR_SET", CSV_GBK);
	
	static Logger log = Logger.getLogger(DataExport.class);
	
	public static String getExportPath() {
		return DMUtil.getAttachPath() + "/export";
	}
    
    public static String exportCSV(List<Object[]> data, List<String> cnFields) {
    	String basePath = getExportPath();
        String exportFileName = System.currentTimeMillis() + ".csv";
		String exportPath = basePath + "/" + exportFileName;
		
		DataExport._exportCSV(exportPath, convertList2Array(data), cnFields );
		return exportFileName;
    }
    
    /**  把 List<Object[]> 转换成 Object[][] 的 */
    public static Object[][] convertList2Array(List<Object[]> list) {
        if (list == null || list.isEmpty()) {
            return new Object[0][0];
        }

        int rowSize = list.size();
        int columnSize = list.get(0).length;
        Object[][] rlt = new Object[rowSize][columnSize];

        for (int i = 0; i < rowSize; i++) {
            Object[] tmpArrays = list.get(i);
            for (int j = 0; j < columnSize; j++) {
                rlt[i][j] = tmpArrays[j];
            }
        }
        return rlt;
    }

    private static void _exportCSV(String path, Object[][] data, List<String> fields) {
    	List<Object[]> list = new ArrayList<Object[]>();
    	for(Object[] temp : data) {
    		list.add(temp);
    	}
    	
    	DataExport.exportCSV(path, list, fields);
    }
 
    public static String exportCSV(String fileName, List<Map<String, Object>> data, List<String> fields) {
    	return exportCSV(fileName, data, fields, SYS_CHAR_SET);
    }
    
    // data 里的Map key可能是code，而fields[x] 可能是name，调用本本方法前需要保证顺序及个数一致
    public static String exportCSV(String fileName, List<Map<String, Object>> data, List<String> fields, String charSet) {
    	List<Object[]> list = new ArrayList<Object[]>();
        for (Map<String, Object> row : data) {
        	list.add( row.values().toArray() );
        }
        
        String exportPath = DataExport.getExportPath() + "/" + fileName;
    	exportCSV(exportPath, list, fields, charSet);
    	
    	return exportPath;
    }
    
    public static void exportCSV(String path, Collection<Object[]> data, List<String> fields) {
    	exportCSV(path, data, fields, SYS_CHAR_SET);
    }
    
    public static void exportCSV(String path, Collection<Object[]> data, List<String> fields, String charSet) {
        try {
        	// 在已经导出的文件上继续导出数据
        	boolean append = fields == null;
        	
        	File file = FileHelper.createFile(path);
            OutputStreamWriter write = new OutputStreamWriter(new FileOutputStream(file, append), charSet );
            BufferedWriter fw = new BufferedWriter(write);   
            
            if( !append ) {
            	fw.write(EasyUtils.list2Str(fields)); // 表头
            	fw.write("\r\n");
            }

            int index = 0;
            for (Object[] row : data) {
            	List<Object> values = new ArrayList<Object>();
            	for(Object value : row) {
            		String valueS = DMUtil.preTreatVal(value); // 对每个含特殊字符（, \r\n \"等）的格子值进行预处理
					values.add(valueS); 
            	}
                fw.write(EasyUtils.list2Str(values));
                fw.write("\r\n");

                if (index++ % 10000 == 0) {
                    fw.flush(); // 每一万行输出一次
                }
            }

            fw.flush();
            fw.close();
            
        } catch (Exception e) {
            throw new BusinessException("export csv error:" + path + ", " + e.getMessage());
        }
    }
    
    // 共Web页面上的表格数据直接导出成csv调用
    public static void exportCSV(String path, String data) {
        try {
        	File file = FileHelper.createFile(path);
            OutputStreamWriter write = new OutputStreamWriter(new FileOutputStream(file), SYS_CHAR_SET );
            BufferedWriter fw = new BufferedWriter(write);   
            
            fw.write(data);
            
            fw.flush();
            fw.close();
            
        } catch (Exception e) {
            throw new BusinessException("export data error:" + path, e);
        }
    }

    /**
     * 使用http请求下载附件。
     * @param sourceFilePath 导出文件路径
     * @param exportName  导出名字
     */
    public static void downloadFileByHttp(HttpServletResponse response, String sourceFilePath) {
    	downloadFileByHttp(response, sourceFilePath, DataExport.SYS_CHAR_SET, false);
    }
    
    public static void downloadFileByHttp(HttpServletResponse response, String sourceFilePath, String charSet, boolean justCSV) {

        File sourceFile = new File(sourceFilePath);
        if( !sourceFile.exists() ) {
        	log.error("download file[" + sourceFilePath + "] not found.");
        	return;
        }
        
    	// 导出XLS文件（先一律转为XLS导出）
    	if( Excel.isCSV(sourceFilePath) && !justCSV) {
    		sourceFilePath = Excel.csv2Excel(sourceFilePath, charSet); 
    		sourceFile = new File(sourceFilePath);
    	}
        
        response.reset();
        response.setCharacterEncoding("utf-8");
        response.setContentType("application/octet-stream"); // 设置附件类型
        response.setContentLength((int) sourceFile.length());
        response.setHeader("Content-Disposition", "attachment; filename=\"" + StringUtil.toUtf8String(sourceFile.getName()) + "\"");
        
        InputStream inStream = null;
        OutputStream outStream = null;
        try {
            outStream = response.getOutputStream();
            inStream = new FileInputStream(sourceFilePath);
            
            byte[] b = new byte[1024];
            int len = inStream.read(b);
            while ( len != -1 ) {
                outStream.write(b, 0, len);
                outStream.flush();
                len = inStream.read(b);
            }           
        } catch (IOException e) {
//            throw new BusinessException("导出时发生IO异常!", e);
        } finally {
        	sourceFile.delete();  // 删除导出目录下面的临时文件
        	FileHelper.closeSteam(inStream, outStream);        
        }
    }
}