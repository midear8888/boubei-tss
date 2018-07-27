/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.sso;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;

import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.ILoginCustomizer;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.modules.log.IBusinessLogger;
import com.boubei.tss.modules.log.Log;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.util.EasyUtils;

/**
 * <p>
 * 登录后将TSS中相关用户对角色信息复制到本地应用的数据库中
 * </p>
 * ILoginCustomizer自定义类还有可能向 RoleUserMapping 写入一些其它地方设置的用户对角色（比如员工表Staff_info）
 */
public class FetchPermissionAfterLogin implements ILoginCustomizer {
    
    ILoginService loginSerivce = (ILoginService) Global.getBean("LoginService");
    IBusinessLogger businessLogger = ((IBusinessLogger) Global.getBean("BusinessLogger"));
    
    /**
     * 加载用户的角色权限信息（用户登录后，角色设置有变化，可单独执行本方法刷新）
     */
    public HttpSession loadRights(Long logonUserId) {

    	// 保存到用户权限（拥有的角色）对应表
        List<Long> roleIds = loginSerivce.saveUserRolesAfterLogin(logonUserId);
        List<String> roleNames = loginSerivce.getRoleNames(roleIds);
        
        // 将用户角色信息塞入到session里        
        HttpSession session = Context.getRequestContext().getSession();
        session.setAttribute(SSOConstants.USER_ID, logonUserId);
        session.setAttribute(SSOConstants.USER_CODE, Environment.getUserCode());
        session.setAttribute(SSOConstants.USER_NAME, Environment.getUserName());
        session.setAttribute(SSOConstants.LOGINNAME_IN_SESSION, Environment.getUserCode());
        
        // 可能会在其它ILoginCustomizer的实现类里取出新增roleId进去，ConcurrentModificationException
        session.setAttribute(SSOConstants.USER_RIGHTS_L, new ArrayList<Long>(roleIds) );  
        session.setAttribute(SSOConstants.USER_RIGHTS_S, EasyUtils.list2Str(roleIds));
        session.setAttribute(SSOConstants.USER_ROLES_L, roleNames);
        session.setAttribute(SSOConstants.USER_ROLES_S, EasyUtils.list2Str(roleNames));
        
        return session;
    }
    
    /**
     * 获取用户所归属的组织信息，通常用于可用于宏代码解析等
     */
    public void loadGroups(Long logonUserId, HttpSession session) {
    	// 获取登陆用户所在父组
        List<Object[]> fatherGroups = loginSerivce.getGroupsByUserId(logonUserId);
        int index = 1, level = fatherGroups.size(); // 层级
        session.setAttribute("GROUP_LEVEL", level);
     
        Object[] lastGroup = new Object[] {-0, "noGroup"};
        String domain = null;
        for(Object[] temp : fatherGroups) {
        	session.setAttribute("GROUP_" + index + "_ID", temp[0]);
        	session.setAttribute("GROUP_" + index + "_NAME", temp[1]);
        	domain = (String) temp[2];
        	index++;
        	
        	lastGroup = temp;
        }
        
        session.setAttribute(SSOConstants.USER_DOMAIN, domain); // 用户所属域
        if( domain != null) { // 取出域下所有用户
        	List<?> users = loginSerivce.getUsersByDomain(domain, "loginName", logonUserId);
        	session.setAttribute(SSOConstants.USERS_OF_DOMAIN, DMUtil.insertSingleQuotes(EasyUtils.list2Str(users)));
        	users = loginSerivce.getUsersByDomain(domain, "id", logonUserId);
        	session.setAttribute(SSOConstants.USERIDS_OF_DOMAIN, EasyUtils.list2Str(users));
        }
        
        session.setAttribute("GROUP_LAST_ID", lastGroup[0]);
    	session.setAttribute("GROUP_LAST_NAME", lastGroup[1]);
    }

    public void execute() {
        Long logonUserId = Environment.getUserId();
        
        HttpSession session = loadRights(logonUserId);
        loadGroups(logonUserId, session);
    	
    	// 记录登陆成功的日志信息
    	Object loginMsg = session.getAttribute("LOGIN_MSG");
    	if( !Environment.isAnonymous() && loginMsg != null ) {
			Log log = new Log(Environment.getUserName(), loginMsg);
        	log.setOperateTable( "用户登录" );
        	businessLogger.output(log);
    	}
    }
}
