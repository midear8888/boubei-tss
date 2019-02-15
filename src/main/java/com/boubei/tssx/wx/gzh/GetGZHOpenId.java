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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import com.boubei.tss.framework.Global;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tssx.wx.WXUtil;

/**
 * 公众号注册登记
 */
@WebServlet(urlPatterns = "/gzh_reg.in")
@SuppressWarnings("unchecked")
public class GetGZHOpenId extends HttpServlet {
	private static final long serialVersionUID = -740569423483772472L;

	Logger log = Logger.getLogger(this.getClass());

	public void init() { }

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html;charset=UTF-8");

		String code  = request.getParameter("code");
		String appId = request.getParameter("appId");
		
		String ret = WXUtil.getOpenId4GZH(code, appId);
		
		Map<String, String> map = (new ObjectMapper()).readValue(ret, Map.class);
		log.info(map);
		if ( !EasyUtils.isNullOrEmpty(map.get("errmsg")) ) {
			response.getWriter().println("获取openid出错");
			return;
		}
		
		// 获取openId/access_token
		String openId = map.get("openid");
		String access_token = map.get("access_token");
		
		Map<String, String> back = new HashMap<String, String>();
		back.put("openId", openId);
		back.put("access_token", access_token);
		
		// 判断该 微信号是否已经绑定手机号
		String hql = "from GZHPhone where openid = ? and appid = ?";
		List<?> bindPhones = Global.getCommonService().getList(hql, openId, appId);
		if (bindPhones.size() == 1) {
			back.put("result", "已绑定");
		} else {
			back.put("result", "未绑定");
		}
		response.getWriter().println(EasyUtils.obj2Json(back));

		// 尝试获取微信个人信息
		ret = WXUtil.getUserInfo4GZH(access_token, openId);
		log.info(ret);
	}

}
