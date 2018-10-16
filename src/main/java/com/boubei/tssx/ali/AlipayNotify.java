package com.boubei.tssx.ali;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.util.BeanUtil;

@WebServlet(urlPatterns="/alinotify.in")
public class AlipayNotify extends HttpServlet {
	
	private static final long serialVersionUID = -740569423483772472L;
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
		doPost(request, response);
    }
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
		
		ICommonService commService = (ICommonService) Global.getBean("CommonService");
		response.setContentType("text/html;charset=UTF-8");
		
		Map<String, String> map = DMUtil.parseRequestParams(request, false);
		String charSet  = map.get("charset");
		String signType = map.get("sign_type");
        String appid    = map.get("auth_app_id");
        String trade_status = map.get("trade_status");
        map.remove("version");
        
		AlipayConfig config = new AlipayConfig(appid);
		
		try {
			boolean signVerified = AlipaySignature.rsaCheckV1(map, config.getAlipayKey(), charSet, signType);
			if( signVerified && "TRADE_SUCCESS".equals(trade_status) ) {
				
				AlipayLog log = new AlipayLog();
				BeanUtil.setDataToBean(log, map);
				commService.create(log);
				
		    	response.getWriter().println("success");
				
			} else {
				response.getWriter().println("fail");
			}
		} 
		catch (AlipayApiException e) {
			throw new BusinessException(e.getMessage(), e);
		}
    }
}
