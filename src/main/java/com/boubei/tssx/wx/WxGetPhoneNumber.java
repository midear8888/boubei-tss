package com.boubei.tssx.wx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.AlgorithmParameters;
import java.security.Security;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.codehaus.jackson.map.ObjectMapper;

@WebServlet(urlPatterns = "/wxphone.in")
public class WxGetPhoneNumber extends HttpServlet {
    
	private static final long serialVersionUID = -740569423483772472L;
	
	Logger log = Logger.getLogger(this.getClass());
	
	private static boolean initialized = false;
	
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
        doPost(request, response);
    }
	
	private static void initialize () {
        if (initialized) return;
        
        Security.addProvider(new BouncyCastleProvider());
        initialized = true;
    }
	
	private String decrypt(byte[] encryptedData, byte[] sessionKey, byte[] iv)
			throws Exception {
		
		initialize();
		
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
        SecretKeySpec sKeySpec = new SecretKeySpec(sessionKey, "AES");
 
        cipher.init(Cipher.DECRYPT_MODE, sKeySpec, generateIV(iv));
        byte[] result = cipher.doFinal(encryptedData);
        
        return new String(result);
    }
	
	private static AlgorithmParameters generateIV(byte[] iv) throws Exception{
        AlgorithmParameters params = AlgorithmParameters.getInstance("AES");
        params.init(new IvParameterSpec(iv));
        return params;
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    		throws ServletException, IOException {
    	
    	response.setContentType("text/html;charset=UTF-8");
    	
    	ServletInputStream stream = request.getInputStream();
    	BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "utf-8"));
    	StringBuffer buffer = new StringBuffer();
    	String temp;
    	while ((temp = reader.readLine()) != null) {
    		buffer.append(temp);
    	}
    	reader.close();
		
		@SuppressWarnings("unchecked")
		Map<String, String> requestMap = (new ObjectMapper()).readValue(buffer.toString(), Map.class);
		
		String jsCode = requestMap.get("jscode");
		String appId  = requestMap.get("appId");
		String _sessionKey = (new WXUtil()).getSessionKey(jsCode, appId);
		
		byte[] encryptedData = Base64.decodeBase64(requestMap.get("encryptedData"));
        byte[] sessionKey = Base64.decodeBase64(_sessionKey);
        byte[] iv = Base64.decodeBase64(requestMap.get("iv"));
        
        try { Thread.sleep(1000); } catch (InterruptedException e1) { }
        
        String phoneNumber = null;
        String result = null;
		try {
			result = decrypt(encryptedData, sessionKey, iv);
			
			@SuppressWarnings("unchecked")
			Map<String, String> resultMap = (new ObjectMapper()).readValue(result, Map.class);
			phoneNumber = resultMap.get("phoneNumber");
		} 
		catch (Exception e) {
			log.error("获取手机号失败, requestMap = " + requestMap + ", \nresult = " + result + ", \nsessionKey = " + _sessionKey, e);
			response.getWriter().println( WXUtil.returnCode(402, e.getMessage()) );
		}
		
    	response.getWriter().println( "{\"phoneNumber\": \"" + phoneNumber + "\"}" );
    }
}
