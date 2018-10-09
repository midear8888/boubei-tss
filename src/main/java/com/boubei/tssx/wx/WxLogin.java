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
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.modules.api.APIService;
import com.boubei.tss.modules.log.IBusinessLogger;
import com.boubei.tss.modules.log.Log;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.um.service.IUserService;
import com.boubei.tss.util.EasyUtils;

/**
 * <p> 微信小程序用户登录Servlet </p>
 * <p>
 * 因普通的Action会被要求登录用户才能访问，所以这里采用Servlet来实现注册功能。
 * 
 * 新建小程序项目注意以下步骤：
 * 1、添加 servicewechat.com 到TSS的IP白名单【系统参数】，否则访问TSS服务时会报404.html(refer= https://servicewechat.com/.....)
 * 2、添加 appId 和 appSecret 到系统参数
 * </p>
 */
@WebServlet(urlPatterns="/wxlogin.in")
public class WxLogin extends HttpServlet {

    private static final long serialVersionUID = -740569423483772472L;
    
    Logger log = Logger.getLogger(this.getClass());
    
    private WxService wxService;
    private APIService apiService;
    private ILoginService loginService;
    private IUserService userService;
    private ICommonService commService;
    
	public void init() {
		wxService = (WxService) Global.getBean("WxService");
		apiService = (APIService) Global.getBean("APIService");
		loginService = (ILoginService) Global.getBean("LoginService");
		userService = (IUserService) Global.getBean("UserService");
		commService = (ICommonService) Global.getBean("CommonService");
	}
	
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
      doGet(request, response);
    }
 
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
    	
    	response.setContentType("text/html;charset=UTF-8");
    	
    	log.debug(DMUtil.getRequestMap(request, false));
    	
    	// 新打开小程序
    	String jscode = request.getParameter("jscode");
    	String appId = request.getParameter("appId");
    	String mobile = request.getParameter("mobile");
    	
    	String ret = null;
    	try {
    		ret = new WXUtil().getOpenId(jscode, appId);
    		
    	} catch (Exception e) {
    		log.error("WXUtil.getOpenId failed: " + e.getMessage());
    		response.getWriter().println( WXUtil.returnCode(503, e.getMessage()) );
    		return;
    	}
    	log.debug("1. WXUtil.getOpenId: " + ret);
		
		@SuppressWarnings("unchecked")
		Map<String, String> m = new ObjectMapper().readValue(ret, Map.class); 
		String openid = m.get("openid");   // EasyUtils.checkNull( m.get("unionId"), m.get("openid") );
		
		User user = wxService.getUserByAuthToken(openid);
		if (user == null) {
			log.info("openid = " + openid + " mapping non-registered user, redirect to reg......" );
        	response.getWriter().println( WXUtil._returnCode(101, ", \"openid\": \"" + openid + "\""));
        	return;
		}
		
		if ( !WXUtil.isNull(mobile) ) { 
			// openid 绑在了一个非本人的账号上，比如绑在了域账号上，后来解除绑定了，但openid还在域账号上
			if( !mobile.equals(user.getLoginName()) && !mobile.equals(user.getTelephone())) {
				log.info("openid = " + openid + " mapping wrong registered user, redirect to reg new one......" );
	        	response.getWriter().println( WXUtil._returnCode(101, ", \"openid\": \"" + openid + "\""));
	        	
	        	user.setAuthToken(null);
	        	commService.update(user);
	        	return;
			}
		}
		
		log.debug("2. openid = " + openid + " mapping user = " + user.getLoginName() );
		
		// 获取用户的角色、组织、域等信息
		apiService.mockLogin( user.getLoginName() );
		Long userId = user.getId();
		String domain = Environment.getDomain();
		String inGroup = Environment.getUserGroup();
		
		/* 扫了网点或网点业务员自有的二维码后也会自动转移到此网点的域下的customer组里，及客户可以在不同域之间移动； 
		 * TODO 在原来域的下单数据咋整？扫原来域二维码移回去可看
		 * TODO 如何防止出现： A belong B, B belong A ？
		 */
        String belong = request.getParameter("belong");
        if( !WXUtil.isNull(belong) && "customer".equals(inGroup) ) {
        	User belongUser = wxService.getBelongUser(belong);
        	if(belongUser != null && !belongUser.getId().equals(user.getBelongUserId())) {
        		Long belongUserId = belongUser.getId();
        		user.setBelongUserId(belongUserId);
        		commService.update(user); 
        		log.debug(user.getUserName() + "变更belongUser为: " + belongUserId);
        		
        		String belongDomain = belongUser.getDomain();
				if( !domain.equals(belongDomain) ) {
        			List<?> list = commService.getList(" from Group o where o.domain = ? and o.name = ? ", belongDomain, "customer");
        			if( list.size() > 0 ) {
        				Group customerGroup = (Group) list.get(0);
            			userService.moveUser(userId, customerGroup.getId());
            			
            			apiService.mockLogin( user.getLoginName() ); // 重新登录一下
        			}
        		}
        	}
        }
        
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
