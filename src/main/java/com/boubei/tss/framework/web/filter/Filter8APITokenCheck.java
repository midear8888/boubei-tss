package com.boubei.tss.framework.web.filter;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.framework.Config;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.SecurityUtil;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.framework.sso.context.RequestContext;
import com.boubei.tss.framework.web.XHttpServletRequest;
import com.boubei.tss.framework.web.wrapper.XHttpServletRequestWrapper;
import com.boubei.tss.modules.api.APIService;
import com.boubei.tss.modules.log.BusinessLogger;
import com.boubei.tss.modules.param.ParamConfig;
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
 * 安全等级 > 6, 必须以签名的方式过滤：md5(secret + timestamp) 或 md5(secret + requestBody.json + timestamp)  timestamp格式 yyyy-MM-dd hh:mi:ss
 * 
 * 登录分：
 * 1、有会话Session: 账号、买吗登录 --> Token，浏览器
 * 2、无会话远程Call: uName / sign(secret) 
 * 
 * 错误code含义：
 *  503  签名为空
 *	504  时间戳无效
 *	505  验签失败
 *	506  令牌验证失败
 *	555  接口内部异常
 */
//@WebFilter(filterName = "Filter8APITokenCheck", urlPatterns = {"/api/*"})
public class Filter8APITokenCheck implements Filter {
	
    static Log log = LogFactory.getLog(Filter8APITokenCheck.class);

    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("Filter8APITokenCheck init in " + Config.getAttribute(PX.ENVIRONMENT));
    }
 
    public void destroy() { }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        
    	request = checkAPIToken( (HttpServletRequest) request );
    	chain.doFilter(request, response);
    }
    
    public static HttpServletRequest checkAPIToken(HttpServletRequest req) {
    	String resourceType = "D1";
    	String resource = req.getServletPath();
 
    	return checkAPIToken(req, resourceType, resource);
	}
	
	/**
	 * 通过uToken令牌，检查指定资源是否被授权给第三方系统访问。For 报表|数据表 的远程调用
	 */
	public static void checkAPIToken(HttpServletRequest req, IResource r) {
	    String resourceType = r.getResourceType(); // D1 | D2
	    String resource = r.getId().toString();

	    checkAPIToken(req, resourceType, resource);
	}
	
	public static HttpServletRequest checkAPIToken(HttpServletRequest req, String resourceType, String resource) {
		
		APIService apiService = (APIService) Global.getBean("APIService");
		
		/* 注：tssJS.ajax需要把uName和uToken放在QueryString里，本过滤器可能在Filter6Decoder前执行（此时XML格式参数还没解析出来）*/
		String uName = getInfo(req, "uName_ALIAS", "uName,appkey");
		String sign  = getInfo(req, "uSign_ALIAS", "uSign,_sign");
	    
	    if( EasyUtils.isNullOrEmpty(uName) ) return req;
	    
	    /* 1、签名验证模式登录 */
	    if( SecurityUtil.isHardestMode() || sign != null ) {
	    	if( EasyUtils.isNullOrEmpty(sign) )  {
	    		throw new BusinessException(EX.DM_11C, 503);
	    	}
	    	
	    	// 检查签名时间戳是否过期（3分钟内有效）
			String timestamp = getInfo(req, "timestamp_ALIAS", "timestamp");
			Date _timestamp = DateUtil.parse(timestamp);
			
			if(_timestamp == null || System.currentTimeMillis() - _timestamp.getTime() > 1000*60*3) {
				throw new BusinessException(EX.parse(EX.DM_11A, timestamp), 504);
			}
			timestamp = DateUtil.formatCare2Second(_timestamp);
			
			// 验签
			List<String> tokenList = apiService.searchTokes(uName, resource, resourceType); 
			Map<String, String> paramsMap = new HashMap<String, String>();
			String requestBody = null;
			try {
				// 原始报文
				requestBody = Filter6XmlHttpDecode.getRequestBody(req.getInputStream());
				paramsMap = EasyUtils.json2Map(requestBody) ;
				
				// req.getInputStream() 只能取一次
				req = XHttpServletRequestWrapper.wrapRequest(req);
		        for (String key : paramsMap.keySet()) {
		        	((XHttpServletRequest)req).addParameter(key, paramsMap.get(key));
		        }
			} catch (Exception e) {
			}
			
			String params = "";
			Map<String, String> resultMap = EasyUtils.sortMapByKey(paramsMap); // 按Key进行排序
	        for (Map.Entry<String, String> entry : resultMap.entrySet()) {
	        	params += entry.getKey() + entry.getValue();
	        }
			for(String secret : tokenList) {
	    		 String _sign1 = InfoEncoder.string2MD5(secret + timestamp);
	    		 String _sign2 = InfoEncoder.string2MD5(secret + params + timestamp);
	    		 if(sign.equalsIgnoreCase(_sign1) || sign.equalsIgnoreCase(_sign2)) {
	    			 apiService.mockLogin(uName);
	    	    	 return req;
	    		 }
	    	}
			
			String content = EX.DM_11B + ", params=" +DMUtil.parseRequestParams(req, false)+ ", tokens=" +tokenList+ ", resource=" +resource+ ", requestBody=" +requestBody;
			log.info( content );
			BusinessLogger.log("对外API", EX.DM_11B, content);
			
			throw new BusinessException(EX.DM_11B, 505);
	    }
		
	    // 2、验证session中token登录（非初次登录）
		RequestContext requestContext = Context.getRequestContext();
		String cToken = requestContext.getUserToken(); // token in cookie
		String sToken = requestContext.getAgoToken(); // token in session
		String uToken = req.getParameter("uToken");
		
		log.debug(resourceType + ", " + resource + ", " + uName + ", " + uToken);
		log.debug("token in cookie  = " + cToken);
		log.debug("token in session = " + sToken);
		log.debug("request sessionID = " + Context.getRequestContext().getSessionId() );
		
		if( cToken != null && cToken.equals(sToken) ) return req; // 非初次登录
	    
		if( EasyUtils.isNullOrEmpty(uToken) ) return req;
	    
		// 3、uNameu + Token登录； 
		List<String> tokenList = apiService.searchTokes(uName, resource, resourceType); // 分别按资源的【ID】+ uName 搜索一遍令牌
		
    	if( tokenList.contains(uToken) ) {
    		apiService.mockLogin(uName);
    	}
    	else {
    		String errorMsg = EX.DM_11 + uName;
        	log.info( errorMsg + ", params=" + DMUtil.parseRequestParams(req, false) + ", tokens=" + tokenList + ", resource=" + resource);
        	
        	/* 如果携带错误的uName和uToken，不抛出异常（在白名单中且允许匿名访问的地址，就算令牌是错的也要能访问）
        	 * throw new BusinessException(errorMsg, 506);
        	 */
    	}
    	return req;
	}
	
	public static String getInfo(HttpServletRequest req, String config, String defaultV) {
		String[] nameAlias = ParamConfig.getAttribute(config, defaultV).split(",");
		String val = null; 
		for(String alias : nameAlias) {
			val = req.getParameter(alias);
			if( !EasyUtils.isNullOrEmpty(val) )  { 
				break;
			}
		}
		return val;
	}
}
