/* ==================================================================   
 * Created [2018-03-14] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tssx.wx;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.persistence.ICommonService;

/**
 * 公众号注册登记
 */
@WebServlet(urlPatterns="/gzh_reg.in")
public class WxRegister4GZH extends HttpServlet {
    private static final long serialVersionUID = -740569423483772472L;
    
    Logger log = Logger.getLogger(this.getClass());
    
    private ICommonService commService;
    
	public void init() {
		commService = (ICommonService) Global.getBean("CommonService");
	}
	
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
      doGet(request, response);
    }
 
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
    	
    	response.setContentType("text/html;charset=UTF-8");
    	
    	log.info(DMUtil.parseRequestParams(request, false));
    	
    	String mobile = request.getParameter("mobile");
    	String code = request.getParameter("code");
    	String appId = request.getParameter("appId");
    	String ret = WXUtil.getOpenId4GZH(code, appId);
    	log.info(ret);
    	
    	@SuppressWarnings("unchecked")
		Map<String, String> map = (new ObjectMapper()).readValue(ret, Map.class);
    	
    	String access_token = map.get("access_token");
    	String openId = map.get("openid");
    	
    	WxGZHBindPhone bindPhone = new WxGZHBindPhone();
    	bindPhone.setMobile(mobile);
    	bindPhone.setOpenid(openId);
    	bindPhone.setAppid(appId);
    	log.info(bindPhone);
    	commService.create(bindPhone);
    	
    	ret = WXUtil.getUserInfo4GZH(access_token, openId);
    	log.info(ret);
    }

}
