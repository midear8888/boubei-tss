/* ==================================================================   
 * Created [2019-1-10] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tssx.web;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.boubei.tss.framework.Config;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.modules.param.ParamConfig;
import com.boubei.tss.util.EasyUtils;

/**
 * 验证码（图形验证码、短信验证码等）过滤器.
 * 
 * 参照relogin：自动登录(autoLoginFilter) --> UserIdentificationException --> 重新输入密码 --> 重发请求
 */
//@WebFilter(filterName = "Filter11CheckCode", urlPatterns = {"/*"})
public class Filter11CheckCode implements Filter {
	
    Logger log = Logger.getLogger(Filter11CheckCode.class);
    
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
    	
    	HttpServletRequest req = (HttpServletRequest) request;
    	HttpServletResponse rep = (HttpServletResponse) response;
    	HttpSession session = req.getSession(true);
    	
    	String servletPath = req.getServletPath();
 
    	// 1、需要图形验证码
    	List<String> list = Arrays.asList( ParamConfig.getAttribute("url.ck_img.list", "").split(",") );
    	if( list.contains(servletPath) ) {
    		// TODO 检查有没有携带验证码，如果没有，则返回needIMGCode标识给请求页面，页面上弹框请求/tss/img/api/ck/randomKey
    		String ckCode = req.getParameter(SSOConstants.RANDOM_KEY);
    		if(ckCode == null) {
    			rep.getWriter().println( "need_img_check_code" );
    			return;
    		}
    		
            Object ckInSession = session.getAttribute(SSOConstants.RANDOM_KEY);
            if(ckCode != null || ckInSession != null) {
            	if( !EasyUtils.obj2String(ckInSession).equals(ckCode) ) {
            		session.removeAttribute(SSOConstants.LOGIN_CHECK_KEY); // 一次验证不成功，就要重新生成验证码(在生成图片验证码时)
            		rep.getWriter().println(  );
            	}
            } 
        	return;
    	}
    	
    	// 2、需要短信验证码
    	list = Arrays.asList( ParamConfig.getAttribute("url.ck_sms.list", "").split(",") );
    	if( list.contains(servletPath) ) {
    		// TODO 检查有没有携带验证码，如果没有，则返回登录人的账号、手机号码给请求页面，页面弹框，用户点击获取短信验证码，然后输入验证码重新发送请求
        	return;
    	}
    	
    	// 3、需要邮件验证码
    	list = Arrays.asList( ParamConfig.getAttribute("url.ck_email.list", "").split(",") );
    	if( list.contains(servletPath) ) {
    		// TODO 检查有没有携带验证码，如果没有，则返回登录人的账号、邮箱给请求页面，页面弹框，用户点击获取短信验证码，然后输入验证码重新发送请求
        	return;
    	}
        
        // 无需验证码
        chain.doFilter(request, response);
    }

	public void init(FilterConfig filterConfig) throws ServletException {
        log.info("Filter11CheckCode init in " + Config.getAttribute("environment"));
    }
    
    public void destroy() { }
}
