/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tssx.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.boubei.tss.EX;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.sms.AbstractSMS;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.framework.web.display.ErrorMessageEncoder;
import com.boubei.tss.framework.web.display.XmlPrintWriter;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.service.IUserService;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tssx.sms.AliyunSMS;

/**
 * <p>
 * 密码忘记时通过手机重置密码：
 * 发送一个随机数到手机短信，用户获取随机数后修改密码。
 * </p>
 */
@WebServlet(urlPatterns="/checkMobile.in")
public class CheckMobile extends HttpServlet {

	private static final long serialVersionUID = 3958707576748004012L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }
 
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String account = request.getParameter("loginName");
		String mobile = request.getParameter("telephone");
		
		if( Context.isOnline() ) {
			account = Environment.getUserCode();
        	mobile = (String) Environment.getUserInfo("telephone");
        	if( !AbstractSMS.isChinaPhoneLegal(mobile) ) {
        		mobile = account;
    		}
        }
		
		IUserService service = (IUserService) Global.getBean("UserService");
		User user = service.getUserByLoginName(account);
		
		response.setContentType("text/html;charset=UTF-8");
		if ( user == null ) {
			ErrorMessageEncoder encoder = new ErrorMessageEncoder( EX.parse(EX.U_41, account) );
			encoder.print(new XmlPrintWriter(response.getWriter()));
		} 
		else {
            String _mobile = user.getTelephone();
            if( !AbstractSMS.isChinaPhoneLegal(_mobile) ) {
            	_mobile = user.getLoginName();
    		}
            if ( EasyUtils.isNullOrEmpty(_mobile) || !_mobile.equals(mobile) || !AbstractSMS.isChinaPhoneLegal(mobile)  ) {
                ErrorMessageEncoder encoder = new ErrorMessageEncoder("手机号码有误或者您账号没有设置正确的手机号码");
                encoder.print(new XmlPrintWriter(response.getWriter()));
            } 
            else {
            	// 产生一个登录随机数，发到用户的手机短信
            	SendSmsResponse ssr = AliyunSMS.instance().sendRandomNum( mobile );
            	if( ssr != null) {
            		int randomKey = EasyUtils.obj2Int(ssr.getMessage());
                    
        			request.getSession(true).setAttribute(SSOConstants.RANDOM_KEY, randomKey);
            	}
			} 
		}
	}
}

	