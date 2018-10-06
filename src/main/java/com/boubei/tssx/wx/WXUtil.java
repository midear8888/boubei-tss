package com.boubei.tssx.wx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.EX;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.modules.param.ParamManager;
import com.boubei.tss.util.EasyUtils;

/**
 * for 微信
 */
@Controller
@RequestMapping("/wx")
public class WXUtil {
	
	public static Map<Integer, String> codeMap = new HashMap<Integer, String>();
	static {
		codeMap.put(100, "register success");
		codeMap.put(101, "user not registered");      // 跳转到注册页，同时返回 openid
		codeMap.put(102, "用户已经在PC端注册，可直接登录"); // 同时返回 uName 和 openid
		codeMap.put(104, "user has registered");      // 通常不会出现，直接登录成功了
		
		codeMap.put(200, "login success");
		
		codeMap.put(400, "域不能为空");
		codeMap.put(401, "域组【${x1}, ${x2}】不存在");
		codeMap.put(402, "获取手机号失败，尝试用验证码注册登录，${x1}");  // 原401
		codeMap.put(403, "手机号不能为空，phone=${x1}"); // 原 103
		codeMap.put(405, "openid不能为空");
		codeMap.put(406, "短信验证码错误或已过期");
		codeMap.put(407, "没有找到可注册的商家或企业域，请向商家或企业的业务员索取专有的小程序二维码再注册");
		
		codeMap.put(503, "获取openid失败：${x1}");
	}
	
	public static String returnCode(int code, Object...params) {
		return _returnCode(code, "", params);
	}
	
	public static String _returnCode(int code, String others, Object...params) {
		String x = code >= 300 ? "error" : "msg";
		String msg = EX.parse(codeMap.get(code) + "", params);
		return "{\"code\": \"" +code+ "\", \"" +x+ "\": \"" +msg+ "\" " +others+ "}";
	}
	
	public static boolean isNull(String param) {
		return EasyUtils.isNullOrEmpty(param) || "null".equals(param.trim()) || "undefined".equals(param.trim());
	}
	
	/**
	 * getOpenId 需要在后台实现
	 * return  {"session_key":"VJGXtUJUG0RYuF6s+Jv2qg==","openid":"oLM2J5TpvHvoZFWjPjPaho9qhXw4"}
	 * @throws IOException 
	 * @throws HttpException 
	 */
	@RequestMapping(value = "/openid")
	@ResponseBody
	public String getOpenId(String jsCode, String appId) throws HttpException, IOException {
		
		String secret = ParamManager.getValue(appId, null);
		if(secret == null) {
			throw new BusinessException("系统参数里没有维护当前小程序的secret.");
		}
		
		String url = "https://api.weixin.qq.com/sns/jscode2session?" +
				"appid=" + appId +
				"&secret=" + secret +
				"&js_code=" + jsCode + 
				"&grant_type=authorization_code";
		
		HttpMethod httpMethod = new GetMethod(url);
		HttpClient httpClient = new HttpClient();
		
		httpClient.executeMethod(httpMethod);
		String responseBody = httpMethod.getResponseBodyAsString();
		String ret = new String(responseBody.getBytes("UTF-8"));
		return ret;
	}
	
	public String getSessionKey (String jscode, String appId) throws IOException {
		String result = getOpenId(jscode, appId);
		
		@SuppressWarnings("unchecked")
		Map<String, String> map = (new ObjectMapper()).readValue(result, Map.class);
		
		String sessionKey = map.get("session_key");
		if( sessionKey == null) {
			throw new BusinessException( map.toString() );
		}
		
		return sessionKey;
	}
	
	public String getAccessToken (String appId) throws HttpException, IOException {
		
		String secret = ParamManager.getValue(appId, null);
		if(secret == null) {
			throw new BusinessException("系统参数里没有维护当前小程序的secret");
		}
		
		String url = "https://api.weixin.qq.com/cgi-bin/token?" +
				"appid=" + appId +
				"&secret=" + secret +
				"&grant_type=client_credential";
		
		HttpMethod httpMethod = new GetMethod(url);
		HttpClient httpClient = new HttpClient();
		
		httpClient.executeMethod(httpMethod);
		String responseBody = httpMethod.getResponseBodyAsString();
		String ret = new String(responseBody.getBytes("UTF-8"));
 
		return ret;
	}

	public String getToken(String appId) throws HttpException, IOException {
		String ret = getAccessToken(appId);
		
		@SuppressWarnings("unchecked")
		Map<String, String> map = (new ObjectMapper()).readValue(ret, Map.class);
		
		return map.get("access_token");
	}
	
	/**
     * 将二进制转换成文件保存
     * 
     * @param is 二进制流
     * @param imgFile 图片的路径
     */
	public static void save2Image(InputStream is, String imgFile) {
		FileOutputStream fos = null;
		try {
			File file = new File(imgFile); // 可以是任何图片格式.jpg,.png等
			fos = new FileOutputStream(file);
			byte[] b = new byte[1024];
			int nRead = 0;
			while ((nRead = is.read(b)) != -1) {
				fos.write(b, 0, nRead);
			}
			fos.flush();
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
				fos.close();
			} catch (IOException e) { }
		}
	}
	
}
