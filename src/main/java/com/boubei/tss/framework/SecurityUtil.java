/* ==================================================================   
 * Created [2009-7-7] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework;

import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import com.boubei.tss.PX;
import com.boubei.tss.cache.Cacheable;
import com.boubei.tss.cache.Pool;
import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.web.filter.Filter8APITokenCheck;
import com.boubei.tss.modules.param.ParamConfig;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.URLUtil;

/**
 * 设置一个安全级别的配置参数，依据相应级别来判断是否要进行XSS清理等安全操作
 * 注：当编辑门户组件时, 需要降低安全级别（Admin账号可绕过XSS清理）。
 */
public class SecurityUtil {
    
	public static String SECURITY_LEVEL = "security.level";
	public static int SECURITY_LEVELS[] = {0, 1, 2, 3, 4, 5, 6, 7};
	public static int LEVEL_1 = SECURITY_LEVELS[1];
	public static int LEVEL_2 = SECURITY_LEVELS[2];
	public static int LEVEL_3 = SECURITY_LEVELS[3];
	public static int LEVEL_4 = SECURITY_LEVELS[4]; // safe
	public static int LEVEL_5 = SECURITY_LEVELS[5]; 
	public static int LEVEL_6 = SECURITY_LEVELS[6]; // hard
	public static int LEVEL_7 = SECURITY_LEVELS[7]; // hardest
	
	public static int getLevel() {
		try {
			return EasyUtils.obj2Int( Config.getAttribute(SECURITY_LEVEL) );
		} catch(Exception e) {
			return SECURITY_LEVELS[0];
		}
	}
	
	public static boolean isSafeMode() {
		return getLevel() >= LEVEL_4;
	}
	
	public static boolean isHardMode() {
		return getLevel() >= LEVEL_6;
	}
	
	public static boolean isHardestMode() {
		return getLevel() >= LEVEL_7;
	}
	
	public static String fuckXSS(String value, HttpServletRequest request) {
        if( !isSafeMode() || Environment.isAdmin() ) {
        	return value;
        }
        
        return _fuckXSS(value, request);
    }
	
    /**
     * 防止XSS攻击
     */
    public static String _fuckXSS(String value, HttpServletRequest request) {
        if (value == null)  return value;
     
        // NOTE: It's highly recommended to use the ESAPI library and uncomment the following line to
        // avoid encoded attacks.
        // value = ESAPI.encoder().canonicalize(value);
    	
        // Avoid null characters
        value = value.replaceAll("", "");
        // Avoid anything between script tags
        Pattern scriptPattern = Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE);
        value = scriptPattern.matcher(value).replaceAll("");
        // Avoid anything in a src="http://www.yihaomen.com/article/java/..." type of e­xpression
        scriptPattern = Pattern.compile("src[\r\n]*=[\r\n]*\\\'(.*?)\\\'", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        value = scriptPattern.matcher(value).replaceAll("");
        scriptPattern = Pattern.compile("src[\r\n]*=[\r\n]*\\\"(.*?)\\\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        value = scriptPattern.matcher(value).replaceAll("");
        // Remove any lonesome </script> tag
        scriptPattern = Pattern.compile("</script>", Pattern.CASE_INSENSITIVE);
        value = scriptPattern.matcher(value).replaceAll("");
        // Remove any lonesome <script ...> tag
        scriptPattern = Pattern.compile("<script(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        value = scriptPattern.matcher(value).replaceAll("");
        // Avoid eval(...) e­xpressions
        scriptPattern = Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        value = scriptPattern.matcher(value).replaceAll("");
        // Avoid e­xpression(...) e­xpressions
        scriptPattern = Pattern.compile("e­xpression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        value = scriptPattern.matcher(value).replaceAll("");
        // Avoid javascript:... e­xpressions
        scriptPattern = Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE);
        value = scriptPattern.matcher(value).replaceAll("");
        // Avoid vbscript:... e­xpressions
        scriptPattern = Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE);
        value = scriptPattern.matcher(value).replaceAll("");
        // Avoid onload= e­xpressions
        scriptPattern = Pattern.compile("onload(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        value = scriptPattern.matcher(value).replaceAll("");
        // Avoid onerror= e­xpressions
        scriptPattern = Pattern.compile("onerror(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        value = scriptPattern.matcher(value).replaceAll("");
        
        return value;
    }
    
    /**
     * 控制单个IP对单个资源的请求频次，及最短间隔时间
     * 
     * DENY_MASS_ATTACK = true
     * MIN_REQUEST_INTERVAL = 100  两次访问间隔不能超过 100ms，默认 0
     * MAX_HTTP_REQUEST = 600      每分钟访问不能超过600次，即10次/秒； 默认 180
     * 
     */
    public static void denyMassAttack(HttpServletRequest request) {
    	if( !"true".equals(ParamConfig.getAttribute(PX.DENY_MASS_ATTACK, "false")) ) {
    		return;
    	}
    	
    	String servletPath = request.getServletPath();
    	if( servletPath.indexOf(".") > 0 && !servletPath.endsWith(".do") && !servletPath.endsWith(".in") ) {
    		return; // 忽略js、css、图片等， 除了 .in, .do, .html外，其它带“.”的均忽略
    	}
    	
    	/* 
    	 * IP白名单内不受限制
    	 * IP黑名单内拒绝访问
    	 */
		Set<String> clientIps = URLUtil.getClientIps(request);
    	String blackIPs = ParamConfig.getAttribute(PX.IP_BLACK_LIST, "");
    	String whiteIPs = ParamConfig.getAttribute(PX.IP_WHITE_LIST, "");
    	for( String ip : clientIps) {
    		if(blackIPs.indexOf(ip) >= 0) {
    			throw new BusinessException("request denied", 403);
    		}
    		if(whiteIPs.indexOf(ip) >= 0) {
    			return;
    		}
    	}
		
		Pool cache = CacheHelper.getShortCache();
    	String uName = Filter8APITokenCheck.getInfo(request, "uName_ALIAS", "uName");
		String key = EasyUtils.obj2String(uName) + "@" + EasyUtils.list2Str(clientIps) + ":" + servletPath;
    	Cacheable item = cache.getObject(key);
    	if( item == null ) {
    		item = cache.putObject(key, new Object());
    	}
    	long firstCall = item.getBirthday(), lastPreCall = item.getPreAccessed(), lastCall = item.getAccessed();
    	int hit = item.getHit();
    	
    	// 同一接口两次访问时间间隔不得小于 MIN_REQUEST_INTERVAL
    	int interval = EasyUtils.obj2Int(ParamConfig.getAttribute(PX.MIN_REQUEST_INTERVAL, "0")); 
    	if( hit > 10 && lastCall - lastPreCall < interval ) {
    		throw new BusinessException("Request too fast, please try again later"); // 请求太快，请稍后再试！
    	}
    	
    	// 每分钟（即60*1000微秒）内请求同一接口次数不得超过 MAX_HTTP_REQUEST （ 默认 每秒不超过 180/60 = 3次， 300ms/次）
    	int maxHttpReqs = EasyUtils.obj2Int(ParamConfig.getAttribute(PX.MAX_HTTP_REQUEST, "180"));
    	float minitus = Math.max(1, (lastCall - firstCall) / 1000);
    	if( hit / minitus  > maxHttpReqs) {
    		throw new BusinessException("Requests per minute are too frequent, please try again later"); // 每分钟请求过于频繁，请稍后再试！
    	}
    }
}
