package com.boubei.tssx.ali;

import com.boubei.tss.modules.param.ParamConfig;
import com.boubei.tss.util.EasyUtils;

public class AlipayConfig {

	static String CAHR_SET = "utf-8";
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

	public String getNotifyUrl(Object afterPaySuccess) {
		String domain = ParamConfig.getAttribute("notify_Url", "www.boudata.com");
		afterPaySuccess = EasyUtils.isNullOrEmpty(afterPaySuccess) ? "" : ("?afterPaySuccess=" + afterPaySuccess);
		return "https://" + domain + "/tss/alinotify.in" + afterPaySuccess;
	}
}
