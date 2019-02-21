/* ==================================================================   
 * Created [2018-03-14] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tssx.wx;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.framework.Global;
import com.boubei.tss.modules.log.IBusinessLogger;
import com.boubei.tss.modules.log.Log;
import com.boubei.tss.modules.param.ParamManager;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tssx.sms.AliyunSMS;

/**
 * <p> 微信小程序用户注册Servlet </p>
 * <p>
 * 因普通的Action会被要求登录用户才能访问，所以这里采用Servlet来实现注册功能。
 * </p>
 * 
 * 客户注册：
 * http://localhost:9000/tss/wxreg.in?domain=CX&group=customer &openid=abc1231&mobile=13588833833
 * http://localhost:9000/tss/wxreg.in?belong=BD0000 &openid=abc1235&mobile=13588833838
 */ 
@WebServlet(urlPatterns="/wxreg.in")
public class WxRegister extends HttpServlet {

    private static final long serialVersionUID = -740569423483772472L;
    
    Logger log = Logger.getLogger(this.getClass());
    
    private WxService wxService;
    
	public void init() {
		wxService = (WxService) Global.getBean("WxService");
	}
	
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
    	doGet(request, response);
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {

    	response.setContentType("text/html;charset=UTF-8");
    	
    	log.info(DMUtil.parseRequestParams(request, false));
    	
    	String _domain= request.getParameter("domain");
    	String domain = _domain;
        String group  = request.getParameter("group");
        String belong = request.getParameter("belong");  // 优先用belong + group, 其次：domain + group
        
        // 检查手机号和openid 有没为空
        String mobile = request.getParameter("mobile");
        String openId = request.getParameter("openid");
        if ( WXUtil.isNull(mobile) ) {
        	response.getWriter().println(  WXUtil.returnCode(403, mobile) );
        	return;
        }
        if ( WXUtil.isNull(openId) ) {
        	String appId = request.getParameter("appId");
        	String jscode = request.getParameter("jscode");
        	if( appId != null && jscode != null) {
        		openId = new WXUtil()._getOpenId(jscode, appId);
        	}
        	
        	if ( WXUtil.isNull(openId) ) {
        		response.getWriter().println(  WXUtil.returnCode(405) );
        		return;
        	}
        }
        
        // 验证码注册（mode=phone）：校验短信验证码smsCode
        String mode = request.getParameter("mode");
        if ("phone".equals(mode)) {
        	String smsCode = request.getParameter("smsCode");
	    	if( EasyUtils.isNullOrEmpty(smsCode) || !AliyunSMS.instance().checkCode( mobile, smsCode ) ) {
	    		response.getWriter().println(  WXUtil.returnCode(406) );
	        	return;
	    	}
        }
       
        // 用户已经在PC端完成注册，或原有authToken被修改了
        User user0 = wxService.checkPhoneNum(mobile);
        if (user0 != null) { 
    		wxService.bindOpenID(user0, openId);
        	response.getWriter().println( WXUtil.returnCode(102, ", \"uName\": \"" + user0.getLoginName() + "\", \"openid\": \"" + openId + "\"") );
        	return;
        }
        
        // 获取belong用户的domain、group
        Long belongUserId = null;
        if ( !WXUtil.isNull(belong) ) { // belong 可以是 id, 账号, 
        	User belongUser = wxService.getBelongUser(belong);
        	if(belongUser != null) {
        		belongUserId = belongUser.getId();
        		domain = belongUser.getDomain();
        		group = (String) EasyUtils.checkNull(group, belongUser.getGroupName()); // 优先用指定的组
        	}
        }  
        // 小程序发布后自动生成的二维码 或 搜索小程序之间进入小程序的，所有参数默认都为空； 默认注册的默认域的客户组里体验
        else if (WXUtil.isNull(domain) && WXUtil.isNull(group) && WXUtil.isNull(belong) ) {
        	domain = _domain = ParamManager.getValue("DEFAULT_REG_DOMAIN", ""); 
        	if(EasyUtils.isNullOrEmpty(domain)) {
        		response.getWriter().println( WXUtil.returnCode(407) );
            	return;
        	}
        	group = "customer";
        }
        
        if (EasyUtils.isNullOrEmpty(domain)) {
        	response.getWriter().println( WXUtil.returnCode(400) );
        	return;
        }
        
        // 注册
        User user = new User();
		user.setLoginName(mobile);
		user.setPassword(mobile);
		user.setUserName( (String) EasyUtils.checkNull(request.getParameter("userName"), mobile) );
		user.setAuthToken(openId);
        user.setOrignPassword(user.getPassword());
        user.setAccountLife(null);          // 默认有效期50年
        user.setBelongUserId(belongUserId); // 上线业务员 或 邀请商家
        String result = wxService.regWxUser(user, domain, group);
        
        // 记录登陆成功的日志信息
  		IBusinessLogger businessLogger = ((IBusinessLogger) Global.getBean("BusinessLogger"));
  		Log log = new Log(user.getUserName(), domain + ", " + group );
        log.setOperatorBrowser("微信");
        log.setOperateTable("用户注册（微信）");
        businessLogger.output(log);
        
        response.getWriter().println( result );
    }

}
