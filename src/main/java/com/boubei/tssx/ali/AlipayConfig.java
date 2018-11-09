package com.boubei.tssx.ali;

import com.boubei.tss.modules.param.ParamConfig;

public class AlipayConfig {
	
	static String Char_Set = "utf-8";
	static String Sign_Type = "RSA2";
	
	String appid;
	
	public AlipayConfig(String appid) {
		this.appid = appid;
	}

	public String getPrivateKey() {
		return ParamConfig.getAttribute(appid + "_app_private_key"); // 应用私钥
	}
	
	public String getPublicKey() {
		return ParamConfig.getAttribute(appid + "_app_public_key"); // 应用公钥
	}
	
	public String getAlipayKey() {
		return ParamConfig.getAttribute(appid + "_alipay_public_key"); // 支付宝公钥
	}
	
	public String getUrl() {
		return "https://openapi.alipay.com/gateway.do";
	}
	
	public String getNotifyUrl() {
		String domain = ParamConfig.getAttribute("notify_Url", "www.boudata.com");
		return "https://"+ domain + "/tss/alinotify.in";
	}
}
