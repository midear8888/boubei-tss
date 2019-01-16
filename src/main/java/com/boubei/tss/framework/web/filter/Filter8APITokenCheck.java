package com.boubei.tss.framework.web.filter;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.boubei.tss.EX;
import com.boubei.tss.PX;
import com.boubei.tss.framework.Config;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.SecurityUtil;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.framework.sso.context.RequestContext;
import com.boubei.tss.modules.api.APIService;
import com.boubei.tss.um.permission.IResource;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.InfoEncoder;

/** 
 * 对外发布接口，调用时令牌检测。(注：地址里必须含 /api/ )
 * 
 * API接口旨在规范TSS开放平台与第三方接入平台的交互方式、交互原则、接口定义。
 * 每个接入方都有唯一的账号和对应的秘钥，这是双方交互过程中唯一的识别依据。
 * 
 * 交互设计原则：
 * 	安全性：对于远程交互,安全第一,需要避免数据在传输过程中被篡改,避免接口被其他方调用, 保证数据的隔离性。对接双方需要保证各自的appkey不外泄。
 * 	稳定性：对接双方需要保证双方交互接口的稳定性,避免`接口不稳定导致通信过程中数据不及时,甚至数据丢失,进而对平台客户产生影响。
 * 	可扩展性：
 * 	高性能：大业务量时,需要交互的接口性能较高,需要双方的接口都能快速处理调用方发送的请求。
 * 
 * http://api?uName=JK&uSign=md5(params+secret+timestamp)&timestamp=&参数1=value1&参数2=value2.......
 * JS 和 Java分别对params进行排序，拼接成json字符串，然后进行签名
 * 
 * 安全等级 > 6, 必须以签名的方式过滤：md5(secret + timestamp)  timestamp格式 yyyy-MM-dd hh:mi:ss
 * 
 * 登录分：
 * 1、有会话Session: 账号、买吗登录 --> Token，浏览器
 * 2、无会话远程Call: uName / sign(secret) 
 * 
 */
//@WebFilter(filterName = "Filter8APITokenCheck", urlPatterns = {"/api/*"})
public class Filter8APITokenCheck implements Filter {
    
    static Log log = LogFactory.getLog(Filter8APITokenCheck.class);

    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("Filter8APITokenCheck init in " + Config.getAttribute(PX.ENVIRONMENT));
    }
 
    public void destroy() { }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        
    	checkAPIToken( (HttpServletRequest) request );
    	chain.doFilter(request, response);
    }
    
    public static void checkAPIToken(HttpServletRequest req) {
    	String resourceType = "D1";
    	String resource = req.getServletPath();
 
    	checkAPIToken(req, resourceType, resource);
	}
	
	/**
	 * 通过uToken令牌，检查指定资源是否被授权给第三方系统访问。For 报表|数据表 的远程调用
	 */
	public static void checkAPIToken(HttpServletRequest req, IResource r) {
	    String resourceType = r.getResourceType(); // D1 | D2
	    String resource = r.getId().toString();

	    checkAPIToken(req, resourceType, resource);
	}
	
	public static void checkAPIToken(HttpServletRequest req, String resourceType, String resource) {
		
		APIService apiService = (APIService) Global.getBean("APIService");
		
		/* 注：tssJS.ajax需要把uName和uToken放在QueryString里，本过滤器可能在Filter6Decoder前执行（此时XML格式参数还没解析出来）*/
		String uName  = req.getParameter("uName");
	    
	    if( EasyUtils.isNullOrEmpty(uName) ) return;
	    
	    /* 签名验证模式 */
	    String sign = req.getParameter("uSign");
	    if( SecurityUtil.isHardestMode() || sign != null ) {
	    	if( EasyUtils.isNullOrEmpty(sign) )  {
	    		throw new BusinessException(EX.DM_11C);
	    	}
	    	
			String timestamp = req.getParameter("timestamp");
			Date _timestamp = DateUtil.parse(timestamp);
			if(_timestamp == null || System.currentTimeMillis() - _timestamp.getTime() > 1000*60*30) {
				throw new BusinessException(EX.parse(EX.DM_11A, timestamp));
			}
			
			List<String> tokenList = apiService.searchTokes(uName, resource, resourceType); 
			for(String secret : tokenList) {
	    		 String _sign = InfoEncoder.string2MD5(secret + timestamp);
	    		 if(sign.equalsIgnoreCase(_sign)) {
	    			 apiService.mockLogin(uName);
	    	    	 return;
	    		 }
	    	}
			throw new BusinessException(EX.DM_11B);
	    }
		
		RequestContext requestContext = Context.getRequestContext();
		String cToken = requestContext.getUserToken(); // token in cookie
		String sToken = requestContext.getAgoToken(); // token in session
		String uToken = req.getParameter("uToken");
		
		log.debug(resourceType + ", " + resource + ", " + uName + ", " + uToken);
		log.debug("token in cookie  = " + cToken);
		log.debug("token in session = " + sToken);
		
		if( cToken != null && cToken.equals(sToken) ) return; // 非初次登录
	    
		if( EasyUtils.isNullOrEmpty(uToken) ) return;
	    
		// 分别按资源的【ID】+ uName 搜索一遍令牌
		List<String> tokenList = apiService.searchTokes(uName, resource, resourceType); 
		
    	if( tokenList.contains(uToken) ) {
    		apiService.mockLogin(uName);
    		return;
    	}
    	
    	throw new BusinessException(EX.DM_11);
	}
}
