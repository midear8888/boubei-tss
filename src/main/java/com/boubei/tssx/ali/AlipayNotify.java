package com.boubei.tssx.ali;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.alipay.api.internal.util.AlipaySignature;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.modules.cloud.pay.AfterPayService;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;

@WebServlet(urlPatterns = "/alinotify.in")
public class AlipayNotify extends HttpServlet {
	
	protected Logger log = Logger.getLogger(this.getClass());

	private static final long serialVersionUID = -740569423483772472L;
	static final String payType = "支付宝";

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		ICommonService commService = (ICommonService) Global.getBean("CommonService");
		response.setContentType("text/html;charset=UTF-8");

		Map<String, String> map = DMUtil.parseRequestParams(request, false);
		String charSet = map.get("charset");
		String signType = map.get("sign_type");
		String appid = map.get("auth_app_id");
		String trade_status = map.get("trade_status");

		String iAfterPayBean = map.remove("afterPaySuccess");

		AlipayConfig config = new AlipayConfig(appid);

		try {
			boolean signVerified = AlipaySignature.rsaCheckV1(map, config.getAlipayKey(), charSet, signType);

			if (signVerified && "TRADE_SUCCESS".equals(trade_status)) {

				String order_no = map.get("out_trade_no");
				System.out.println("alipay callback, out_trade_no = " + order_no);
				
				map.remove("version");
				
				AlipayLog log = new AlipayLog();
				BeanUtil.setDataToBean(log, map);
				commService.create(log);

				// 没有传 afterPaySuccess 则不启动回调，用于cloud模块以外地方回调
				if (!EasyUtils.isNullOrEmpty(iAfterPayBean)) {
					AfterPayService afterPayService = (AfterPayService) Global.getBean(iAfterPayBean);
					Double receipt_amount = EasyUtils.obj2Double(map.get("receipt_amount"));
					String buyer_id = map.get("buyer_id");
					afterPayService.handle(order_no, receipt_amount, buyer_id, payType, map);
				}

				response.getWriter().println("success");

			} else {
				response.getWriter().println("fail");
			}
		} catch (Exception e) {
			response.getWriter().println("fail");
			log.error("alipay callback error: " + e.getMessage(), e);
		}
	}
}
