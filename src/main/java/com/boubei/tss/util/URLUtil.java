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

import eu.bitwalker.useragentutils.Browser;
import eu.bitwalker.useragentutils.OperatingSystem;
import eu.bitwalker.useragentutils.UserAgent;
import eu.bitwalker.useragentutils.Version;

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
        
        return getURL(path, onePathUrl.getProtocol());
    }
    
    public static URL getURL(String path, String protocol) {
    	try {
            return new URL(protocol, null, 0, path);
        } catch (MalformedURLException e) {
            throw new RuntimeException("getURL path:" + path + " failed.", e);
        }
    }
    
    /**
     * 定位项目中classes目录路径
     * @return
     */
    public static URL getClassesPath() {
        URL onePathUrl = getOnePathUrl();
        String path = onePathUrl.getPath();
        path = path.substring(0, path.length() - 9);
        
        return getURL(new File(path).getParent(), onePathUrl.getProtocol());
    }
    
    /**
     * 自主解析Get Request queryString里包含的参数，防止Tomcat下乱码
     */
    public static Map<String, String> parseQueryString(String queryString) {
    	
    	Map<String, String> paramsMap = new HashMap<String, String>();
    	
    	if( !EasyUtils.isNullOrEmpty(queryString) ) {
	    	try {
	    		queryString = queryString.replaceAll("\\+", "plusjia");  // URLDecoder.decode会把 + 号吃掉
				queryString = URLDecoder.decode(queryString, "UTF-8");
				if( queryString.indexOf("%") >= 0 ) {
					queryString = URLDecoder.decode(queryString, "UTF-8"); 
				}
				// 防止前台encodeURI了两次， encodeURI(" ") ==> 20%  vs  encodeURI(encodeURI(" "))  ==> "%2520"
				queryString = queryString.replaceAll("plusjia", "\\+");
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
    
    static String[] browsers = {"qqbrowser", "metasr", "2345explorer", "ubrowser", "360"};
    public static String parseBrowser(String useragent) {
    	useragent = (useragent + "").toLowerCase();
    	if(useragent.indexOf("micromessenger") >= 0 && useragent.indexOf("qqbrowser") < 0) { // 微信客户端，注意区分QQ浏览器
    		return "微信";
    	} 
    	
    	String origin = useragent;
    	try {
			UserAgent userAgent = UserAgent.parseUserAgentString( useragent );
        	Browser browser = userAgent.getBrowser();
        	OperatingSystem opsys = userAgent.getOperatingSystem(); // 访问设备系统
        	Version browserVersion = userAgent.getBrowserVersion(); // 详细版本
            String version = browserVersion.getMajorVersion();      // 浏览器主版本
            
            origin = browser.getGroup().getName() + version + "-" + opsys;
            
        } catch(Exception e) { }
        	
    	for(String _b : browsers) {
    		if(useragent.indexOf(_b) >= 0) {
    			origin += "," + _b;
    			break;
    		}
    	}
        return origin.toLowerCase();
    }
}
