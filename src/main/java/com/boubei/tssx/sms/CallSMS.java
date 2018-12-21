package com.boubei.tssx.sms;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.util.EasyUtils;

@WebServlet(urlPatterns="/sms.in")
public class CallSMS extends HttpServlet {
 
	private static final long serialVersionUID = -572025963643040390L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
    	doGet(request, response);
    }
 
    /* 
     * $.post( '/tss/sms.in', {'phone': '13588833834'});
     * $.post( '/tss/sms.in', {'phone': '13588833834', 'tlCode': tlCode, 'smsMsg': smsMsg});
     * 
     * TODO 防止同一客户端切换手机号反复申请
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
    	
    	String smsMsg;
    	String queryString = request.getQueryString();
		if( queryString != null && queryString.indexOf("smsMsg") >= 0 ) { // wx call
			smsMsg = DMUtil.parseRequestParams(request, true).get("smsMsg");
		} else {
			smsMsg = request.getParameter("smsMsg");
		}
    	
    	String phone  = request.getParameter("phone");
    	String tlCode = request.getParameter("tlCode");
    	String tlParam = smsMsg;
    	
		if( !EasyUtils.isNullOrEmpty(smsMsg) && !EasyUtils.isNullOrEmpty(tlCode)  ) {
			AliyunSMS.instance().send(phone, tlCode, tlParam, -1);
		}
		else { // 默认发送验证码
			AliyunSMS.instance().sendRandomNum( phone );
		}
    }

}
