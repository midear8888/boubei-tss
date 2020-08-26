/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.framework.sso.identifier.BaseUserIdentifier;
import com.boubei.tss.framework.web.display.XmlPrintWriter;
import com.boubei.tss.framework.web.display.xmlhttp.XmlHttpEncoder;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.util.BeanUtil;

/**
 * <p>
 * 通过用户登录名，获取用户认证方式及用户名<br>
 * </p>
 */
@WebServlet(urlPatterns="/getLoginInfo.in")
public class GetLoginInfo extends HttpServlet {
    private static final long serialVersionUID = 8680769606094382553L;
 
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }
 
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ILoginService service = (ILoginService) Global.getBean("LoginService");
        
        String loginName = request.getParameter(SSOConstants.USER_ACCOUNT);
        try {
        	User user = service.getLoginInfoByLoginName(loginName);
        	String clazz = user.getAuthMethod();
        	 
            XmlHttpEncoder encoder = new XmlHttpEncoder(); 
            encoder.put("UserName",  user.getUserName());  // 返回用户姓名
			encoder.put("identifier", clazz);  // 返回身份认证器类名：全路径
            
			BaseUserIdentifier identifier = (BaseUserIdentifier) BeanUtil.newInstanceByName(clazz);
            HttpSession session = request.getSession(true);
			identifier.before(user, encoder, session);
            
            response.setCharacterEncoding("utf-8");
            encoder.print(new XmlPrintWriter(response.getWriter()));
        } 
        catch(BusinessException e) {
        	throw new BusinessException(e.getMessage(), false); // 无需打印登录异常
        }
    }

}
