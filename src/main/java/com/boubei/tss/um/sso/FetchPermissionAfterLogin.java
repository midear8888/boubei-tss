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
import java.util.Map;

import javax.servlet.http.HttpSession;

import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.ILoginCustomizer;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.modules.log.IBusinessLogger;
import com.boubei.tss.modules.log.Log;
import com.boubei.tss.um.helper.dto.OperatorDTO;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.um.sso.online.DBOnlineUser;
import com.boubei.tss.util.EasyUtils;

/**
 * <p>
 * 登录后将TSS中相关用户对角色信息复制到本地应用的数据库中
 * </p>
 * ILoginCustomizer自定义类还有可能向 RoleUserMapping 写入一些其它地方设置的用户对角色（比如员工表Staff_info）
 */
public class FetchPermissionAfterLogin implements ILoginCustomizer {
    
    ILoginService loginService = (ILoginService) Global.getBean("LoginService");
    ICommonService commonService = Global.getCommonService();
    IBusinessLogger businessLogger = ((IBusinessLogger) Global.getBean("BusinessLogger"));
    
    /**
     * 加载用户的角色权限信息（用户登录后，角色设置有变化，可单独执行本方法刷新）
     */
    public HttpSession loadRights(Long logonUserId) {

    	// 保存到用户权限（拥有的角色）对应表
        List<Long> roleIds = loginService.saveUserRolesAfterLogin(logonUserId);
        List<String> roleNames = loginService.getRoleNames(roleIds);
        
        // 将用户角色信息塞入到session里        
        HttpSession session = Context.getRequestContext().getSession();
        session.setAttribute(SSOConstants.USER_ID, logonUserId);
        session.setAttribute(SSOConstants.USER_CODE, Environment.getUserCode());
        session.setAttribute(SSOConstants.USER_NAME, Environment.getUserName());
        session.setAttribute(SSOConstants.USER_ACCOUNT, Environment.getUserCode());
        
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
        List<Object[]> fatherGroups = loginService.getGroupsByUserId(logonUserId);
        int index = 1, level = fatherGroups.size(); // 层级
        session.setAttribute("GROUP_LEVEL", level);
        session.setAttribute("GROUPS_MAIN", fatherGroups);
        session.setAttribute("GROUPS_ASSIT", loginService.getAssistGroupIdsByUserId(logonUserId));
        
        Object[] lastGroup = new Object[] {-0L, "noGroup"};
        String domain = null;
        for(Object[] temp : fatherGroups) {
        	session.setAttribute("GROUP_" + index + "_ID", temp[0]);
        	session.setAttribute("GROUP_" + index + "_NAME", temp[1]);
        	domain = (String) temp[2];
        	index++;
        	
        	lastGroup = temp;
        }
        
        if( !"noGroup".equals(lastGroup[1]) ) {
        	Long inGroup = (Long) lastGroup[0];
			List<OperatorDTO> users = loginService.getUsersByGroupId(inGroup, false);
        	String list = EasyUtils.list2Str(EasyUtils.objAttr2List(users, "loginName"));
			session.setAttribute(SSOConstants.USERS_OF_GROUP, DMUtil.insertSingleQuotes(list));
        	session.setAttribute(SSOConstants.USERIDS_OF_GROUP, EasyUtils.list2Str(EasyUtils.objAttr2List(users, "id")));
        	
        	users = loginService.getUsersByGroupId(inGroup, true);
        	list = EasyUtils.list2Str(EasyUtils.objAttr2List(users, "loginName"));
			session.setAttribute(SSOConstants.USERS_OF_GROUP_DEEP, DMUtil.insertSingleQuotes(list));
        	session.setAttribute(SSOConstants.USERIDS_OF_GROUP_DEEP, EasyUtils.list2Str(EasyUtils.objAttr2List(users, "id")));
        }
        
        session.setAttribute(SSOConstants.USER_DOMAIN, domain); // 用户所属域
        if( domain != null) { // 取出域下所有用户
        	List<?> users = loginService.getUsersByDomain(domain, "loginName", logonUserId);
        	session.setAttribute(SSOConstants.USERS_OF_DOMAIN, DMUtil.insertSingleQuotes(EasyUtils.list2Str(users)));
        	users = loginService.getUsersByDomain(domain, "id", logonUserId);
        	session.setAttribute(SSOConstants.USERIDS_OF_DOMAIN, EasyUtils.list2Str(users));
        	
        	// 修改在线用户中的domain值
        	List<?> ouList = commonService.getList("from DBOnlineUser where userId = ?", logonUserId);
        	for( Object t : ouList ) {
        		DBOnlineUser ou = (DBOnlineUser) t;
				ou.setDomain(domain);
				commonService.update(ou);
        	}
        }
        
        session.setAttribute("GROUP_LAST_ID", lastGroup[0]);
    	session.setAttribute("GROUP_LAST_NAME", lastGroup[1]);
    	session.setAttribute(SSOConstants.USER_GROUP_ID, lastGroup[0]);
    	session.setAttribute(SSOConstants.USER_GROUP, lastGroup[1]);

    	// 获取用户所属域的功能模块信息
    	List<Map<String,Object>> list;
    	if(logonUserId == -1L){
    		list = SQLExcutor.queryL("select * from cloud_module_def");
    	}else{
    		list = SQLExcutor.queryL("select distinct s.* from cloud_module_def s,cloud_module_user t where s.id = t.moduleid and t.domain = ? ", domain);
    		//TODO 正式使用2.0版本，需对功能模块加有效期限制 sql：and t.expiredate > ?   param：DateUtil.addDays(new Date(), -1)
    	}
		List<Object> modules = new ArrayList<Object>();
		List<Object> moduleNames = new ArrayList<Object>();
		for(Map<String,Object> map : list){
			modules.add(map.get("code"));
			moduleNames.add(map.get("module"));
		}
		session.setAttribute(SSOConstants.USER_MODULE_C, modules);
	    session.setAttribute(SSOConstants.USER_MODULE_N, moduleNames);
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
