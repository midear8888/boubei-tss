/* ==================================================================   
 * Created [2018-03-14] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tssx.wx.gzh;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import com.boubei.tss.util.EasyUtils;
import com.boubei.tssx.wx.WXUtil;

/**
 * 公众号注册登记
 */
@WebServlet(urlPatterns = "/gzh_userinfo.in")
@SuppressWarnings("unchecked")
public class GetGZHUserInfo extends HttpServlet {
	private static final long serialVersionUID = -740569423483772472L;

	Logger log = Logger.getLogger(this.getClass());

	public void init() {
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html;charset=UTF-8");

		String code = request.getParameter("code");
		String appId = request.getParameter("appId");

		String ret = WXUtil.getOpenId4GZH(code, appId);

		Map<String, String> map = (new ObjectMapper()).readValue(ret, Map.class);
		log.info(map);
		if (!EasyUtils.isNullOrEmpty(map.get("errmsg"))) {
			response.getWriter().println("获取openid出错");
			return;
		}

		String openId = map.get("openid");
		String access_token = map.get("access_token");

		// 尝试获取微信个人信息
		ret = WXUtil.getUserInfo4GZH(access_token, openId);

		map = (new ObjectMapper()).readValue(ret, Map.class);
		log.info(map);
		if (!EasyUtils.isNullOrEmpty(map.get("errmsg"))) {
			response.getWriter().println("获取用户信息出错");
			return;
		} else {
			response.getWriter().println(ret);
		}
	}

}
