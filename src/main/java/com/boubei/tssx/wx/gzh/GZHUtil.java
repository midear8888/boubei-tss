package com.boubei.tssx.wx.gzh;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.json.JSONObject;

public class GZHUtil {
	
	 static Logger log = Logger.getLogger(GZHUtil.class);
	 
	/**
var params_ = {
    appid:'wxd1e32997a92e1630', 
    phone:'13588833834', 
    template_id:'ysKQILCBscDF2Hbx36WKWnfMxn08HbsJPgPPutVTeEQ',  
    miniprogram: {
      appid: '',
      pagepath: ''
    }, 
    data: {
      productType: { value: '商品名' },
      name: { value: '微信数据容灾服务', color: '#173177' },
      number: { value: '1份' },
      expDate: { value: '2014-09-12' },
      remark: { value: '如有疑问，请致电13912345678联系我们，或回复M来了解详情。' }
    } 
}

params_.data = JSON.stringify(params_.data).replace(/#/g,"%23"), 
params_.miniprogram = JSON.stringify(params_.miniprogram);

$.ajax({
    url: '/tss/wx/api/sendgzhmsg?uName=BD0000&uToken=oKYA65HP9aMry2lgcQgyorxYXasU',  
    headers: {appCode:'BD'},    
    params: params_,   
    success: function(data){
        console.log( data)
    } 
})
	 */
	public static String sendMessage(Map<String, String> params) {
		String url = "https://www.boudata.com/tss/wx/api/sendgzhmsg";
		PostMethod postMethod = new PostMethod(url);
		postMethod.setRequestHeader("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
		postMethod.setRequestHeader("Referer", "https://e8.boudata.com");
		
		postMethod.addParameter("uName", "BD0000");
		postMethod.addParameter("uToken", "oKYA65HP9aMry2lgcQgyorxYXasU" );
		for( String key : params.keySet() ) {
			postMethod.addParameter( key, params.get(key) );
		}

		HttpClient httpClient = new HttpClient();
		try {
			httpClient.executeMethod(postMethod);
			return postMethod.getResponseBodyAsString();
		} catch (Exception e) {
			log.error("发送公众号通知失败", e);
			return e.getMessage();
		}
	}
	
	public static void main(String[] args) {
		JSONObject data = new JSONObject();
		JSONObject jo = new JSONObject();
		jo.put("value", "商品名");
		data.put("productType", jo);
		
		jo = new JSONObject();
		jo.put("value", "微信数据容灾服务");
		jo.put("color", "#173177");
		data.put("name", jo);
		
		jo = new JSONObject();
		jo.put("value", "1份");
		data.put("number", jo);
		
		jo = new JSONObject();
		jo.put("value", "2014-09-12");
		data.put("expDate", jo);
		
		jo = new JSONObject();
		jo.put("value", "如有疑问，请致电13912345678联系我们，或回复M来了解详情。");
		data.put("remark", jo);
		
		JSONObject miniprogram = new JSONObject();
		miniprogram.put("appid", "");
		miniprogram.put("pagepath", "");
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("appid", "wxd1e32997a92e1630");
		params.put("phone", "13588833834");
		params.put("template_id", "ysKQILCBscDF2Hbx36WKWnfMxn08HbsJPgPPutVTeEQ");
		params.put("data", data.toString());
		params.put("miniprogram", miniprogram.toString());
		
		GZHUtil.sendMessage( params );
	}
	
}
