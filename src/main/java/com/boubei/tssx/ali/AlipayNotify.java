package com.boubei.tssx.ali;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alipay.api.internal.util.AlipaySignature;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.modules.cloud.product.AfterPayService;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;

@WebServlet(urlPatterns = "/alinotify.in")
public class AlipayNotify extends HttpServlet {

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

				System.out.println("ali access, out_trade_no = " + map.get("out_trade_no"));

				AlipayLog log = new AlipayLog();
				map.remove("version");
				BeanUtil.setDataToBean(log, map);
				commService.create(log);

				if (!EasyUtils.isNullOrEmpty(iAfterPayBean)) {
					AfterPayService afterPayService = (AfterPayService) Global.getBean(iAfterPayBean);
					afterPayService.handle(map, payType);
				}

				response.getWriter().println("success");

			} else {
				response.getWriter().println("fail");
			}
		} catch (Exception e) {
			throw new BusinessException(e.getMessage(), e);
		}
	}
}
