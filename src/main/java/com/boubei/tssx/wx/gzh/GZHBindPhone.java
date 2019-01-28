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

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tssx.sms.AliyunSMS;
import com.boubei.tssx.wx.WXUtil;

/**
 * 公众号注册登记
 */
@WebServlet(urlPatterns = "/gzh_bind.in")
public class GZHBindPhone extends HttpServlet {
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

		String mobile = request.getParameter("mobile");
		String smsCode = request.getParameter("smsCode");
		String appId = request.getParameter("appId");
		String openId = request.getParameter("openId");

		if (!EasyUtils.isNullOrEmpty(mobile) && (EasyUtils.isNullOrEmpty(smsCode) || !AliyunSMS.instance().checkCode(mobile, smsCode))) {
			response.getWriter().println(WXUtil.returnCode(406));
			return;
		}

		GZHPhone bindPhone = new GZHPhone();
		bindPhone.setMobile(mobile);
		bindPhone.setOpenid(openId);
		bindPhone.setAppid(appId);
		commService.create(bindPhone);

		response.getWriter().println(WXUtil._returnCode(200, ", \"error\": \"" + "\""));
	}

}
