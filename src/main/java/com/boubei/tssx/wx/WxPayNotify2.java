package com.boubei.tssx.wx;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.modules.cloud.pay.AfterPayService;
import com.boubei.tss.util.EasyUtils;

@WebServlet(urlPatterns = "/wxnotify2.in")
public class WxPayNotify2 extends HttpServlet {

	ICommonService commService;

	private static final long serialVersionUID = -740569423483772472L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String iAfterPayBean = request.getParameter("afterPaySuccess");
		String out_trade_no = request.getParameter("out_trade_no");
		Double receipt_amount = EasyUtils.obj2Double(request.getParameter("receipt_amount"));

		System.out.println("wx access, out_trade_no = " + out_trade_no);
		
		iAfterPayBean = (String) EasyUtils.checkNull(iAfterPayBean, "CloudService");
		AfterPayService afterPayService = (AfterPayService) Global.getBean(iAfterPayBean);
		afterPayService.handle(out_trade_no, receipt_amount, "self", "微信", null);

	}
}
