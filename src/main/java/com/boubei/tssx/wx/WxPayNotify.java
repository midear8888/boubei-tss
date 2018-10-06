package com.boubei.tssx.wx;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.util.BeanUtil;
import com.github.wxpay.sdk.WXPay;

@WebServlet(urlPatterns="/wxnotify.in")
public class WxPayNotify extends HttpServlet {
	
	ICommonService commService;

	private static final long serialVersionUID = -740569423483772472L;
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
		doPost(request, response);
    }
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
		
		commService = (ICommonService) Global.getBean("CommonService");
		
		response.setContentType("text/html;charset=UTF-8");
		
		Map<String, String> map = new HashMap<String, String>();
	    InputStream inputStream = request.getInputStream();
	    
	    SAXReader reader = new SAXReader();
	    Document document = null; 
		try {
			document = reader.read(inputStream);
		} catch (DocumentException e) {
			throw new BusinessException(e.getMessage(), e);
		}
	    Element root = document.getRootElement();
	    
	    @SuppressWarnings("unchecked")
		List<Element> elementList = root.elements();
	 
	    for (Element e : elementList){
	        map.put(e.getName(), e.getText());
	    }

	    inputStream.close();
	    inputStream = null;
		
		String return_code = map.get("return_code");
		String return_msg = map.get("return_msg");
		
		if ("SUCCESS".equals(return_code)){
			
			try {
				WXPayConfigImpl config = new WXPayConfigImpl(map.get("appid"), map.get("mch_id"));
				WXPay wxpay = new WXPay(config);

		        if (wxpay.isPayResultNotifySignatureValid(map)) {
		        	
		        	WxPayLog log = new WxPayLog();
					BeanUtil.setDataToBean(log, map);
			    	
			    	log.setCoupon_type(map.get("coupon_type_$n"));
			    	log.setCoupon_id(map.get("coupon_id_$n"));
			    	log.setCoupon_fee_single(map.get("coupon_fee_$n"));
			    	
			    	response.getWriter().println("<xml>" +
		                    "<return_code><![CDATA[SUCCESS]]></return_code>" +
		                    "<return_msg><![CDATA[OK]]></return_msg>" +
		                    "</xml>");
			    	
			    	commService.create(log);
			    	return;
		        }
		         
			} catch (Exception e) {
				throw new BusinessException(e.getMessage(), e);
			}
		}
	    	
		response.getWriter().println("<xml>" +
                    "<return_code><![CDATA[FAIL]]></return_code>" +
                    "<return_msg><![CDATA[" + return_msg + "]]></return_msg>" +
                    "</xml>");
    }
}
