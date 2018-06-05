package com.boubei.tss.framework.web.filter;

import java.io.IOException;
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
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.modules.api.APIService;
import com.boubei.tss.um.permission.IResource;

/** 
 * 对外发布接口，调用时令牌检测。
 * 
 * API接口旨在规范TSS开放平台与第三方接入平台的交互方式、交互原则、接口定义。
 * 每个接入方都有唯一的公司编码和对应的appkey，这是双方交互过程中唯一的识别依据。
 * 
 * 交互设计原则：
 * 	安全性：对于远程交互,安全第一,需要避免数据在传输过程中被篡改,避免接口被其他方调用, 保证数据的隔离性。对接双方需要保证各自的appkey不外泄。
 * 	稳定性：对接双方需要保证双方交互接口的稳定性,避免`接口不稳定导致通信过程中数据不及时,甚至数据丢失,进而对平台客户产生影响。
 * 	可扩展性
 * 	高性能：大业务量时,需要交互的接口性能较高,需要双方的接口都能快速处理调用方发送的请求。
 * 
 * http://api?uName=JK&sign=md5(params+secret+timestamp)&timestamp=&参数1=value1&参数2=value2.......
 * 
 */
//@WebFilter(filterName = "Filter8APITokenCheck", urlPatterns = {"/api/*"})
public class Filter8APITokenCheck implements Filter {
    
    static Log log = LogFactory.getLog(Filter8APITokenCheck.class);

    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("Filter8APITokenCheck init in " + Context.getApplicationContext().getCurrentAppCode());
    }
 
    public void destroy() { }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        
    	autoLogin((HttpServletRequest) request);
    	
    	chain.doFilter(request, response);
    }

    // tssJS.ajax需要把uName和uToken放在QueryString里，本过滤器可能Filter6Decoder前执行
	public static void autoLogin(HttpServletRequest req) {
		
        String uName  = req.getParameter("uName");
        String uToken = req.getParameter("uToken");
        
        if( uToken == null || uName == null ) return; // 两个都传了才检测
        
        if( uToken != null && uName != null ) {
        	APIService apiService = (APIService) Global.getBean("APIService");
        	String resourceType = "D1";
        	String servletPath = req.getServletPath();
    		
    		//  搜索令牌
    		List<String> tokenList = apiService.searchTokes(uName, servletPath, resourceType); 
        	if( tokenList.contains(uToken) ) {
        		apiService.mockLogin(uName, uToken);
        		log.debug(servletPath + ", " + uName + ", api token pass");
        	}
        }
	}
	
	/**
	 * 通过uToken令牌，检查指定资源是否被授权给第三方系统访问。For 报表|数据表 的远程调用
	 */
	public static void checkAPIToken(HttpServletRequest req, IResource r) {
		
		String uName  = req.getParameter("uName");
	    String uToken = req.getParameter("uToken");
		
	    if( uToken == null && uName == null ) return; // 只要传了其中一个就检测，只传一个则抛出异常
	    
	    APIService apiService = (APIService) Global.getBean("APIService");
	    String resourceType = r.getResourceType(); // D1 | D2
	    
		// 分别按资源的【ID】或【名称】 + uName 搜索一遍令牌
		List<String> tokenList = apiService.searchTokes(uName, r.getId().toString(), resourceType); 
		tokenList.addAll( apiService.searchTokes(uName, r.getName(), resourceType) );
		
    	if( tokenList.contains(uToken) ) {
    		apiService.mockLogin(uName, uToken);
    		return;
    	}
    	
    	throw new BusinessException(EX.DM_11);
	}
}