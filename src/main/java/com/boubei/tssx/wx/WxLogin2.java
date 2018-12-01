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
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.modules.api.APIService;
import com.boubei.tss.modules.log.IBusinessLogger;
import com.boubei.tss.modules.log.Log;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.util.EasyUtils;

/**
 * <p> 微信小程序用户登录Servlet for 审核测试 </p>
 */
@WebServlet(urlPatterns="/wxlogin_test.in")
public class WxLogin2 extends HttpServlet {

    private static final long serialVersionUID = -740569423483772472L;
    
    Logger log = Logger.getLogger(this.getClass());
    
    private WxService wxService;
    private APIService apiService;
    private ILoginService loginService;
    
	public void init() {
		wxService = (WxService) Global.getBean("WxService");
		apiService = (APIService) Global.getBean("APIService");
		loginService = (ILoginService) Global.getBean("LoginService");
	}
	
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
      doGet(request, response);
    }
 
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
    	
    	response.setContentType("text/html;charset=UTF-8");
    	
    	log.debug(DMUtil.parseRequestParams(request, false));
    	
    	String uName = request.getParameter("uName");
    	String openid = request.getParameter("uToken");
    	User user = wxService.getUserByAuthToken(openid);
    	
    	if( !user.getLoginName().equals(uName) ) return;
		
		// 获取用户的角色、组织、域等信息
		apiService.mockLogin( user.getLoginName() );
		Long userId = user.getId();
		String domain = Environment.getDomain();
		
		if( !"CS".equals(domain) ) return;
        
		log.debug("3. mock login in ");
		
		List<Object[]> groups = loginService.getGroupsByUserId(userId);  //List<[groupId, groupName]>，截掉"主用户组"
		List<Object[]> assistGroups = loginService.getAssistGroupIdsByUserId(userId); // List<[groupId, groupName]>
		
		HttpSession session = Context.getRequestContext().getSession();
		Object roles = session.getAttribute(SSOConstants.USER_ROLES_S);
		
		// 记录登陆成功的日志信息
		IBusinessLogger businessLogger = ((IBusinessLogger) Global.getBean("BusinessLogger"));
		Log log = new Log(user.getUserName(), roles + " | " + EasyUtils.list2Str(groups, 1) );
        log.setOperateTable( "用户登录（微信）" );
        log.setOperatorBrowser("微信");
        businessLogger.output(log);
		
		response.getWriter().println(  
				WXUtil._returnCode(200,  "," + 
					" \"roles\": \"" + roles + "\", " +
					" \"groups\": \"" + EasyUtils.list2Str(groups, 1) + "\", " +
					" \"assistGroups\": \"" + EasyUtils.list2Str(assistGroups, 1) + "\", " +
					" \"uName\": \"" + user.getLoginName() + "\", " +
					" \"cnName\": \"" + user.getUserName() + "\", " +
					" \"domain\": \"" + domain + "\", " +
					" \"belongUserId\": \"" + user.getBelongUserId() + "\", " +
					" \"openid\": \"" + openid + "\""
				) 
			);
    }

}
