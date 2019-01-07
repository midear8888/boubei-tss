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
import java.util.List;
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
import com.boubei.tss.util.EasyUtils;
import com.boubei.tssx.wx.WXUtil;

/**
 * 公众号注册登记
 */
@WebServlet(urlPatterns = "/gzh_reg.in")
@SuppressWarnings("unchecked")
public class WxRegister4GZH extends HttpServlet {
	private static final long serialVersionUID = -740569423483772472L;

	Logger log = Logger.getLogger(this.getClass());

	private ICommonService commService;

	public void init() {
		commService = (ICommonService) Global.getBean("CommonService");
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html;charset=UTF-8");

		log.info(DMUtil.parseRequestParams(request, false));

		String code = request.getParameter("code");
		String appId = request.getParameter("appId");
		String ret = WXUtil.getOpenId4GZH(code, appId);
		
		Map<String, String> map = (new ObjectMapper()).readValue(ret, Map.class);
		log.info(map);
		if (!EasyUtils.isNullOrEmpty(map.get("errmsg"))) {
			response.getWriter().println("获取openid出错");
			return;
		}

		String access_token = map.get("access_token");
		String openId = map.get("openid");
		
		// 判断该 微信号是否已经绑定手机号
		List<WxGZHBindPhone> bindPhones = (List<WxGZHBindPhone>) commService.getList("from WxGZHBindPhone where openid=? and appid=?",
				openId, appId);
		log.info(bindPhones.size());
		if (bindPhones.size() == 1) {
			response.getWriter().println("已绑定");
		} else {
			response.getWriter().println(openId);
		}

		ret = WXUtil.getUserInfo4GZH(access_token, openId);
		log.info(ret);
	}

}
