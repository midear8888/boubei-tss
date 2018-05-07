/* ==================================================================   
 * Created [2006-6-19] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018  
 * ================================================================== 
*/
package com.boubei.tss.util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.helpers.Loader;

/** 
 * <p> URLUtil.java </p> 
 * 
 * 相对路径转换成绝对路径
 * 
 * @author Jon.King 2007-4-8
 */
public class URLUtil {
	/**
	 * 获取资源文件的绝对路径
	 * 
	 * @param file
	 * @return
	 */
	public static URL getResourceFileUrl(String file) {
		if(file == null) return null;
		
		URL url = Loader.getResource(file);
		if (url == null) {
			url = ClassLoader.class.getResource(file);
		}
		return url;
	}
    
    /**
     * 此处利用了文件“application.properties”来定位。
     */
    private static URL getOnePathUrl() {
    	return URLUtil.getResourceFileUrl("application.properties"); 
    }
    
    /**
     * <p>
     * 获取Web文件的绝对路径
     * </p>
     * @param file web文件的相对路径，相对与"WEB-INF"的父目录
     * @return 
     */
    public static URL getWebFileUrl(String file) {
        URL onePathUrl = getOnePathUrl();
        
        String path = onePathUrl.getPath();
        int index = path.lastIndexOf("WEB-INF");

        // 没有WEB-INF目录，可能是单元测试环境
        index = (Integer) EasyUtils.checkTrue(index >= 0, index, path.lastIndexOf("target") + 7);
        path = path.substring(0, index) + file;
        
        try {
            onePathUrl = new URL(onePathUrl.getProtocol(), null, 0, path);
        } catch (MalformedURLException e) {
            throw new RuntimeException("getWebFileUrl方法定位path: " + path + " 失败", e);
        }
        return onePathUrl;
    }
    
    /**
     * 定位项目中classes目录路径
     * @return
     */
    public static URL getClassesPath() {
        URL onePathUrl = getOnePathUrl();
        String path = onePathUrl.getPath();
        path = path.substring(0, path.length() - 9);
        
        try {
            onePathUrl = new URL(onePathUrl.getProtocol(), null, 0, new File(path).getParent());
        } catch (MalformedURLException e) {
            throw new RuntimeException("getClassesPath方法定位path: " + path + " 失败", e);
        }
        return onePathUrl;
    }
    
    /**
     * 自主解析Get Request queryString里包含的参数，防止Tomcat下乱码
     */
    public static Map<String, String> parseQueryString(String queryString) {
    	
    	Map<String, String> paramsMap = new HashMap<String, String>();
    	
    	if( !EasyUtils.isNullOrEmpty(queryString) ) {
	    	try {
				queryString = URLDecoder.decode(queryString, "UTF-8");
				queryString = URLDecoder.decode(queryString, "UTF-8"); 
				// 防止前台encodeURI了两次， encodeURI(" ") ==> 20%  vs  encodeURI(encodeURI(" "))  ==> "%2520"
			} 
	    	catch (UnsupportedEncodingException e) { }
	    	
			String[] arr = EasyUtils.split(queryString, "&");
			for(String pairs : arr) {
				int index = pairs.indexOf('=');
				if(index <= 0) continue;
				
				String key = pairs.substring(0, index);
				String val = pairs.substring(index + 1);
				paramsMap.put( key, val );
			}
    	}
		
		return paramsMap;
    }
}
