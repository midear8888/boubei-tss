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
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.MacrocodeCompiler;
import com.boubei.tss.util.URLUtil;
import com.boubei.tss.util.XMLDocUtil;

public class SqlConfig {

	public static final String SQL_CONFIG_INIT = "SQL_CONFIG_INIT";

	static final Logger log = Logger.getLogger(SqlConfig.class);

	static Pool cache = CacheHelper.getNoDeadCache();

	static Map<String, Object> sqlNestFmParams = new HashMap<String, Object>(); // SQL嵌套解析用

	public static String getScript(String sqlCode) {
		return getScript(sqlCode, "script");
	}
	
	public static String getScript(String sqlCode, String sqlPath) {
		return _getScript(sqlCode, sqlPath).sql;
	}

	public static Script _getScript(String sqlCode, String sqlPath) {
		sqlPath = (String) EasyUtils.checkNull(sqlPath, "script");
		String cacheKey = SQL_CONFIG_INIT + "_" + sqlPath.toUpperCase();

		if (!cache.contains(cacheKey)) {
			File scriptDir = new File(URLUtil.getResourceFileUrl(sqlPath).getPath());
			List<File> sqlFiles = FileHelper.listFilesByTypeDeeply("xml", scriptDir);

			for (File sqlFile : sqlFiles) {
				Document doc = XMLDocUtil.createDocByAbsolutePath(sqlFile.getPath());
				Element root = doc.getRootElement();
				String rootRole = root.attributeValue("role");

				List<Element> sqlNodes = XMLDocUtil.selectNodes(doc, "//sql");
				for (Element sqlNode : sqlNodes) {
					Script obj = new Script();
					obj.code = sqlNode.attributeValue("code").trim();
					obj.role = sqlNode.attributeValue("role");
					obj.ds   = sqlNode.attributeValue("datasource");
					obj.dataProcess = sqlNode.attributeValue("bIDataProcess");
					obj.noLog = "true".equalsIgnoreCase( sqlNode.attributeValue("noLog") );
					obj.sql = sqlNode.getText().trim();
					
					obj.name = (String) EasyUtils.checkNull(sqlNode.attributeValue("name"), obj.code);
					obj.role = EasyUtils.obj2String(rootRole) + "," + EasyUtils.obj2String(obj.role);

					sqlNestFmParams.put("${" + obj.code + "}", obj.sql);
					cache.putObject(obj.code, obj);
				}
			}
			cache.putObject(cacheKey, true);
		}

		Cacheable cacheItem = cache.getObject(sqlCode);
		if (cacheItem == null) {
			throw new BusinessException("没有找到编码为【" + sqlCode + "】的SQL");
		}
		Script obj = (Script) cacheItem.getValue();

		// 根据当前登录人的角色判断其是否有权限访问sqlCode对应的SQL
		List<String> ownRoles = Environment.getOwnRoleNames();
		String[] permitRoles = EasyUtils.obj2String( obj.role ).split(",");

		boolean flag = true;
		for (String permitRole : permitRoles) {
			if (EasyUtils.isNullOrEmpty(permitRole))
				continue;

			flag = false;
			if (ownRoles.contains(permitRole)) {
				flag = true;
				break;
			}
		}
		if (!flag) {
			throw new BusinessException("你对数据服务【" + sqlCode + "】没有访问权限");
		}

		obj.sql = MacrocodeCompiler.run(obj.sql, sqlNestFmParams, true); // 自动解析script里的宏嵌套

		return obj;
	}
	
	public static class Script {
		public String code;
		public String sql;
		public String role;
		public String name;
		public String ds;
		public String dataProcess;
		public boolean noLog;
		
		public String toString() {
			return sql;
		}
	}
}
