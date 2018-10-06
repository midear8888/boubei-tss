package com.boubei.tssx.ali;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.boubei.tss.AbstractTest4;

public class AlipayTest extends AbstractTest4 {
	
	@Autowired AlipayAPI alipay;

	@Test
	public void test() throws Exception {
		
		request.addParameter("appid", "2018051160132356");
		request.addParameter("out_trade_no", "1000000");
		request.addParameter("product_code", "FAST_INSTANT_TRADE_PAY");
		request.addParameter("total_amount", "1");
		request.addParameter("subject", "1234");
		request.addParameter("body", "1234");
		alipay.pagepay(request, response);
		
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
		request.addParameter("appid", "2018051160132356");
		request.addParameter("out_trade_no", "10000");
		alipay.query(request, response);
		
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
		request.addParameter("auth_app_id", "2018051160132356");
		request.addParameter("charset", "utf-8");
		request.addParameter("sign_type", "RSA2");
		request.addParameter("trade_status", "TRADE_SUCCESS");
		try {
			new AlipayNotify().doGet(request, response);
		} 
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}
