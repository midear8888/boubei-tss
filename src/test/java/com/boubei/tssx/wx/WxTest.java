package com.boubei.tssx.wx;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartHttpServletRequest;

import com.boubei.tss.AbstractTest4;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.modules.param.ParamManager;
import com.boubei.tss.util.EasyUtils;

public class WxTest extends AbstractTest4 {
 
	String appId = "wx5255074da90a4dca";

	@Before
	public void setUp() throws Exception {
		super.setUp();
		
	    request = new MockMultipartHttpServletRequest();
	    response = new MockHttpServletResponse();
	    
	    ParamManager.addSimpleParam(ParamConstants.DEFAULT_PARENT_ID, appId, appId, "77549f42672ed2fb354a92fbbabef461");
	}
	
	@Test
    public void testWxGetPhoneNumber() {
		WxGetPhoneNumber servlet = new WxGetPhoneNumber();
        try {
        	Map<String, String> params = new HashMap<>(); 
        	params.put("appId", appId);
        	params.put("jscode", "021OHFt91D5CaR1ufIt91jv1u91OHFtM");
        	params.put("encryptedData", "uBoJZwpfUzN+WcW7cyqAJlGfBRKC/Lty8DnuLL96BByGikZR0Rz+TmWNFPKryowwLDWmSuUeFjfz/aqRwvGBbFv16DAUd2u37nFNol7s24JUD2o7n+vgxolYsKIhYKusHdkTXPjJh6u9zs6HU/Ekv/Y69nHFBFgGqVw6a0xQJ4eUicR8IFnSe1+A6xsLBh8ZcoAauyrWMMhhNrOxGt03Vg==");
        	params.put("iv", "WAHtErUeh+iTG/XZDa+EkA==");
        	
        	
        	request.setContent( EasyUtils.obj2Json(params).getBytes() );
        	servlet.doPost(request, response);
            
        } catch (Exception e) {
        	e.printStackTrace();
        	Assert.assertFalse("Test servlet error:" + e.getMessage(), true);
        } finally {
        	servlet.destroy();
        }
    }

	@Test
	public void testGetOpenId() throws Exception {
		String jsCode = "033DJiS82U06rS0lD3U82J2qS82DJiSO";
		
		String url = "https://api.weixin.qq.com/sns/jscode2session?" +
				"appid=" + appId +
				"&secret=" + "9c2699929102fb8754563f8f9d4080d2" +
				"&js_code=" + jsCode + 
				"&grant_type=authorization_code";
		
		HttpMethod httpMethod = new GetMethod(url);
		HttpClient httpClient = new HttpClient();
		
		httpClient.executeMethod(httpMethod);
		String responseBody = httpMethod.getResponseBodyAsString();
		String ret = new String(responseBody.getBytes("UTF-8"));
		
		System.out.println(ret);
	}
	
	@Test
	public void testGetAccessToken() throws Exception {
		String appId = "wxe968d611516359f3";
		
		String url = "https://api.weixin.qq.com/cgi-bin/token?" +
				"appid=" + appId +
				"&secret=" + "9c2699929102fb8754563f8f9d4080d2" +
				"&grant_type=client_credential";
		
		HttpMethod httpMethod = new GetMethod(url);
		HttpClient httpClient = new HttpClient();
		
		httpClient.executeMethod(httpMethod);
		String responseBody = httpMethod.getResponseBodyAsString();
		String ret = new String(responseBody.getBytes("UTF-8"));
		
		System.out.println(ret);
	}
}
