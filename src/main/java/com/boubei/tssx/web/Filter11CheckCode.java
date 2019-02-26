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
import com.boubei.tss.framework.sms.AbstractSMS;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.modules.param.ParamConfig;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MailUtil;
import com.boubei.tss.util.MathUtil;

/**
 * 验证码（图形验证码、短信验证码等）过滤器.
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
    	
    	// 1、需要短信验证码
    	if( checkURLList("url.ck_sms.list", servletPath) ) {
    		// 检查有没有携带验证码，如果没有，则返回登录人的账号、手机号码给请求页面，页面弹框，用户点击获取短信验证码，然后输入验证码重新发送请求
    		String ckCode = req.getParameter(SSOConstants.RANDOM_KEY);
    		if(ckCode == null) {
    			rep.getWriter().print( "need_sms_check_code" );
    			return;
    		}
    		
    		String telephone = (String) Environment.getUserInfo("telephone");
    		if( !AbstractSMS.isChinaPhoneLegal(telephone) ) {
    			telephone = Environment.getUserCode();
    		}
    		if( !AbstractSMS.isChinaPhoneLegal(telephone) ) {
    			print(rep, "当前操作需要手机短信验证，您没有维护手机号，请先添加。");
    			return;
    		}
        	
    		Object ckInSession = session.getAttribute(SSOConstants.RANDOM_KEY);
            if(ckCode != null || ckInSession != null) {
            	if( !EasyUtils.obj2String(ckInSession).equals(ckCode) ) {
            		print(rep, "短信验证码校验失败，请重新输入" );
            		return;
            	}
            } 
    	}
    	
    	// 2、需要邮件验证码
    	else if( checkURLList("url.ck_email.list", servletPath) ) {
    		String ckCode = req.getParameter(SSOConstants.RANDOM_KEY);
    		if(ckCode == null) {
    			String email = (String) Environment.getUserInfo("email");
    			if( EasyUtils.isNullOrEmpty(email) ) {
    				print(rep, "当前操作需要邮箱验证，您没有维护邮箱，请先添加。");
        			return;
        		}
    			
	    		// 检查有没有携带验证码，如果没有，则产生一个登录随机数，发到用户的邮箱里。同时页面弹框，用户输入验证码重新发送请求
	            int randomKey = MathUtil.randomInt6();
				session.setAttribute(SSOConstants.RANDOM_KEY, randomKey);
	        	String info = "您的验证码为：" + randomKey;
	        	MailUtil.sendHTML("验证码确认", info, new String[] { email }, MailUtil.DEFAULT_MS);
	        	
	        	rep.getWriter().print( "need_email_check_code"  );
	        	return;
    		}
    	}
 
    	// 3、需要图形验证码
    	else if( checkURLList("url.ck_img.list", servletPath) ) {
    		// 检查有没有携带验证码，如果没有，则返回needIMGCode标识给请求页面，页面上弹框请求/tss/img/api/ck/randomKey
    		String ckCode = req.getParameter(SSOConstants.RANDOM_KEY);
    		if(ckCode == null) {
    			rep.getWriter().print( "need_img_check_code" );
    			return;
    		}
    		
            Object ckInSession = session.getAttribute(SSOConstants.RANDOM_KEY);
            if(ckCode != null || ckInSession != null) {
            	if( !EasyUtils.obj2String(ckInSession).equals(ckCode) ) {
            		session.removeAttribute(SSOConstants.RANDOM_KEY); // 一次验证不成功，就要重新生成验证码(在生成图片验证码时)
            		rep.getWriter().print( "need_img_check_code" );
            		return;
            	}
            } 
    	}
        
        // 无需验证码 或 校验通过
        chain.doFilter(request, response);
    }
    
    private void print(HttpServletResponse response, String msg) throws IOException {
    	response.setContentType("text/html;charset=utf-8");
		response.getWriter().print("<Response><script>tssJS('#abn').text('" +msg+ "')</script></Response>");
    }
    
    private boolean checkURLList(String url_config, String servletPath) {
    	List<String> list = Arrays.asList( ParamConfig.getAttribute(url_config, "").split(",") );
    	for(String url : list) {
    		if( url.trim().length() > 0 && servletPath.indexOf(url) >= 0 ) {
    			return true;
    		}
    	}
    	
    	return false;
    }

	public void init(FilterConfig filterConfig) throws ServletException {
        log.info("Filter11CheckCode init in " + Config.getAttribute("environment"));
    }
    
    public void destroy() { }
}
