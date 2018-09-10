/* ==================================================================   
 * Created [2016-3-5] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.dml;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;

import com.boubei.tss.cache.Cacheable;
import com.boubei.tss.cache.Pool;
import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.MacrocodeCompiler;
import com.boubei.tss.util.URLUtil;
import com.boubei.tss.util.XMLDocUtil;

public class SqlConfig {
	
	private static final Logger log = Logger.getLogger(SqlConfig.class);
	
	static Pool cache = CacheHelper.getNoDeadCache();
	
	static Map<String, Object> sqlNestFmParams = new HashMap<String, Object>(); // SQL嵌套解析用
	
	public static String getScript(String sqlCode) {
		if( sqlNestFmParams.isEmpty() ) {
			File sqlDir = new File(URLUtil.getResourceFileUrl("script").getPath());
			List<File> sqlFiles = FileHelper.listFilesByTypeDeeply("xml", sqlDir);
			
			for(File sqlFile : sqlFiles) {
				Document doc = XMLDocUtil.createDocByAbsolutePath(sqlFile.getPath());
				List<Element> sqlNodes = XMLDocUtil.selectNodes(doc, "//sql");
				for(Element sqlNode : sqlNodes) {
					String code = sqlNode.attributeValue("code").trim();
					String sql  = sqlNode.getText().trim();
					
					sqlNestFmParams.put("${" + code + "}", sql);
					cache.putObject(code, sql);
				}
			}
		}
		
		Cacheable cacheItem = cache.getObject(sqlCode);
		if(cacheItem == null) {
			log.error("没有找到编码为【" + sqlCode + "】的SQL。");
			return null;
		}
		
		String script = (String) cacheItem.getValue();
		script = MacrocodeCompiler.run(script, sqlNestFmParams, true); // 自动解析script里的宏嵌套
		
		return script;
	}
}
