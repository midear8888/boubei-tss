package com.boubei.tssx;

import org.junit.Test;

import com.boubei.tss.AbstractTest4;

public class ImageCodeAPITest extends AbstractTest4 {

	@Test
	public void test() throws Exception {
		ImageCodeAPI api = new ImageCodeAPI();
		
		String code = "123456";
		api.createCKCodeImg(code, request, response);
		
		request.addParameter("size", "1.2");
		api.createBarCodeImg(code, request, response);
		
		api.createQrBarCodeImg(code, request, response);
	}
}
