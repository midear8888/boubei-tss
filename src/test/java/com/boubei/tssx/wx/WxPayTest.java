package com.boubei.tssx.wx;

import org.junit.Test;

import com.boubei.tss.AbstractTest4;

public class WxPayTest extends AbstractTest4 {

	@Test
	public void test() throws Exception {
		
		WxAPI wxApi = new WxAPI();
		
		request.addParameter("appid", "wx32ecfcea6f822096");
		request.addParameter("touser", "oNi3W5XOKp6GoFtHoX3iKa5gxyRg");
		request.addParameter("template_id", "X-4GDTiI44LCx8FpoFiVFzQQZPPXCVDJMtc2FSm9KyY");
		request.addParameter("form_id", "41ebe0d6aab9f7ba3689aea2c3d7f34c");
		request.addParameter("page", "pages/detail/detail?code=31807020012");
		request.addParameter("data", "{\"keyword1\": {\"value\": \"豪邦物流\"},\"keyword2\": {\"value\": \"31807020012\"},\"keyword3\": {\"value\": \"您的订单已提交成功\"}}");
		
//		request.addParameter("body", "test");
//		request.addParameter("out_trade_no", "1003");
//		request.addParameter("total_fee", "2");
//		request.addParameter("spbill_create_ip", "123.12.12.123");
//		request.addParameter("trade_type", "NATIVE");
//		request.addParameter("product_id", "1");
//		request.addParameter("appid", "wx32ecfcea6f822096");
//		request.addParameter("mchid", "1503974521");
		
		//wxApi.unifiedorder(request, response);
        //wxApi.doOrderClose(request, response);
		wxApi.sendMessage(request, response);

	}
}
