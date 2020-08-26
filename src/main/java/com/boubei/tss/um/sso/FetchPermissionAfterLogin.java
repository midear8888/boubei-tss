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
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSession;

import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.persistence.ICommonDao;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.ILoginCustomizer;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.modules.log.BusinessLogger;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.Role;
import com.boubei.tss.um.helper.dto.GroupDTO;
import com.boubei.tss.um.helper.dto.OperatorDTO;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.util.EasyUtils;

/**
 * <p>
 * 登录后将TSS中相关用户对角色信息复制到本地应用的数据库中
 * </p>
 * ILoginCustomizer自定义类还有可能向 RoleUserMapping 写入一些其它地方设置的用户对角色（比如员工表Staff_info）
 */
public class FetchPermissionAfterLogin implements ILoginCustomizer {
    
    static ILoginService loginService = (ILoginService) Global.getBean("LoginService");
    ICommonDao commonDao = (ICommonDao) Global.getBean("CommonDao");
    
    /**
     * 加载用户的角色权限信息（用户登录后，角色设置有变化，可单独执行本方法刷新）
     */
    public HttpSession loadRights(Long logonUserId, Set<Long> roleIds, Set<String> roleNames) {

    	// 保存到用户权限（拥有的角色）对应表
    	roleIds.addAll( loginService.saveUserRoleMapping(logonUserId) ) ;
    	roleNames.addAll( loginService.getRoleNames(roleIds) );
        
        // 将用户角色信息塞入到session里        
        HttpSession session = Context.getRequestContext().getSession();
        session.setAttribute(SSOConstants.USER_ID, logonUserId);
        session.setAttribute(SSOConstants.USER_CODE, Environment.getUserCode());
        session.setAttribute(SSOConstants.USER_NAME, Environment.getUserName());
        session.setAttribute(SSOConstants.USER_ACCOUNT, Environment.getUserCode());
        
        // 可能会在其它ILoginCustomizer的实现类里取出新增roleId进去，ConcurrentModificationException
        session.setAttribute(SSOConstants.USER_RIGHTS_L, new ArrayList<Long>(roleIds) );  
        session.setAttribute(SSOConstants.USER_RIGHTS_S, EasyUtils.list2Str(roleIds) );
        session.setAttribute(SSOConstants.USER_ROLES_L,  new ArrayList<String>(roleNames) );
        session.setAttribute(SSOConstants.USER_ROLES_S,  EasyUtils.list2Str(roleNames) );
        
        return session;
    }
    
    /**
     * 获取用户所归属的组织信息，通常用于可用于宏代码解析等
     */
    public void loadGroups(Long logonUserId, HttpSession session, Set<Long> roleIds, Set<String> roleNames) {
    	// 获取登陆用户所在父组
        List<Group> fatherGroups = loginService.getGroupsByUserId(logonUserId);
        List<Object[]> mainGroups = new ArrayList<>();
        int index = 1, level = fatherGroups.size(); // 层级
        session.setAttribute("GROUP_LEVEL", level);
        session.setAttribute("GROUPS_ASSIT", loginService.getAssistGroups(logonUserId));
        
        List<Long> fatherGroupIDs = new ArrayList<Long>();
        List<String> fatherGroupNames = new ArrayList<String>();
        String domain = null, domainCN = null, lastGroupName = null;
        Long lastGroupId = -0L;
        
        for(Group temp : fatherGroups) {
        	Long gid = temp.getId();
        	String gname = temp.getName();
        	
			session.setAttribute("GROUP_" + index + "_ID", gid);
			session.setAttribute("GROUP_" + index + "_NAME", gname);
        	fatherGroupIDs.add(gid);
        	fatherGroupNames.add(gname);
        	domain = temp.getDomain();
        	if (temp.isDomainGroup() ) {
        		domainCN = gname;
        	}
        	
        	index++;
        	
        	lastGroupId = gid;
        	lastGroupName = gname;
        	
        	mainGroups.add( new Object[] {temp.getId(), temp.getName()} );
        }
        session.setAttribute("GROUPS_MAIN", mainGroups);
        session.setAttribute("GROUPS_MAIN_ID", fatherGroupIDs);
        session.setAttribute("GROUPS_MAIN_NAME", fatherGroupNames);
        
		if( lastGroupName != null ) {
			List<OperatorDTO> users = loginService.getUsersByGroupId(lastGroupId, false);
        	String list = EasyUtils.objAttr2Str(users, "loginName");
			session.setAttribute(SSOConstants.USERS_OF_GROUP, DMUtil.insertSingleQuotes(list));
        	session.setAttribute(SSOConstants.USERIDS_OF_GROUP, EasyUtils.objAttr2Str(users, "id"));
        	
        	users = loginService.getUsersByGroupId(lastGroupId, true);
        	list = EasyUtils.objAttr2Str(users, "loginName");
			session.setAttribute(SSOConstants.USERS_OF_GROUP_DEEP, DMUtil.insertSingleQuotes(list));
        	session.setAttribute(SSOConstants.USERIDS_OF_GROUP_DEEP, EasyUtils.objAttr2Str(users, "id"));
        	
        	List<GroupDTO> sonGroups = loginService.getGroupTreeByGroupId(lastGroupId);
        	String sonGroupNames = EasyUtils.objAttr2Str(sonGroups, "name");
        	session.setAttribute(SSOConstants.SON_GROUP_TREE, sonGroupNames);
			session.setAttribute(SSOConstants.SON_GROUP_TREE_, DMUtil.insertSingleQuotes(sonGroupNames));
        }
        
        session.setAttribute(SSOConstants.USER_DOMAIN, domain); // 用户所属域
        session.setAttribute(SSOConstants.USER_DOMAIN_CN, EasyUtils.obj2String(domainCN)); 
        if( domain != null ) {
        	// 取出域下所有用户
        	List<?> users = loginService.getUsersByDomain(domain, "loginName", logonUserId);
        	session.setAttribute(SSOConstants.USERS_OF_DOMAIN, DMUtil.insertSingleQuotes(EasyUtils.list2Str(users)));
        	users = loginService.getUsersByDomain(domain, "id", logonUserId);
        	session.setAttribute(SSOConstants.USERIDS_OF_DOMAIN, EasyUtils.list2Str(users));
        	
        	// 读取DomainInfo表里的信息
    		List<Map<String, Object>> x = SQLExcutor.queryL("select * from x_domain where domain = ?", Environment.getDomain());
    		if( x.size() > 0 ) {
				Map<String, Object> domainInfo = x.get(0);
				for (String key : domainInfo.keySet()) {
					List<String> ignores = Arrays.asList("domain,version,id,createtime,creator,updator,updatetime".split(","));
					if (!ignores.contains(key)) {
						session.setAttribute("domain_" + key, domainInfo.get(key));
					}
				}
    		}
    		
    		// 获取用户所属域的功能模块信息（购买模块所有策略都过期的话，不算）
    		List<Object> modules = new ArrayList<Object>();
    		List<Object> moduleNames = new ArrayList<Object>();
    		
        	String sql = "select distinct s.code, s.module, s.inner_base_role "
        			+ " from cloud_module_def s, cloud_module_user t, um_sub_authorize sa "
        			+ " where s.id = t.moduleid and t.domain = ? and t.moduleId = sa.moduleId and t.userId = sa.buyerId"
        			+ " and sa.endDate > now() ";
        	
        	List<?> list = commonDao.getEntitiesByNativeSql(sql, domain); // 单元测试下，原生SQL才能查到 Hibernate 实体数据
    		for(Object obj : list) {
    			Object[] a = (Object[]) obj;
    			modules.add( a[0] );
    			moduleNames.add( a[1] );
    			
    			/* 将模块的默认基础角色自动授予登录人 */
    			List<String> inner_base_roles = EasyUtils.toList( (String) a[2] ) ;
    			for(String rid : inner_base_roles) {
    				Long roleId = EasyUtils.obj2Long(rid);
    				if( !roleIds.contains(roleId) ) {
    					Role role = (Role) commonDao.getEntity(Role.class, roleId );
    					addRole2Session(session, logonUserId, role, roleIds, roleNames);
    				}
    			}
    		}
    		
    		session.setAttribute(SSOConstants.USER_MODULE_C, modules);
    	    session.setAttribute(SSOConstants.USER_MODULE_N, moduleNames);
        }
        
        session.setAttribute("GROUP_LAST_ID", lastGroupId);
    	session.setAttribute("GROUP_LAST_NAME", lastGroupName);
    	session.setAttribute(SSOConstants.USER_GROUP_ID, lastGroupId);
    	session.setAttribute(SSOConstants.USER_GROUP, lastGroupName);
	}
    
    public static void addRole2Session(HttpSession session, Long logonUserId, Role role, Collection<Long> roleIds, Collection<String> roleNames) {
		Long roleId = role.getId();
		roleIds.add( roleId );
		roleNames.add( role.getName() );
		
		loginService.saveUserRoleMapping(logonUserId, new ArrayList<Long>(roleIds));
		
		session.setAttribute(SSOConstants.USER_RIGHTS_L, new ArrayList<Long>(roleIds) );
		session.setAttribute(SSOConstants.USER_ROLES_L, new ArrayList<String>(roleNames) );
		session.setAttribute(SSOConstants.USER_RIGHTS_S, EasyUtils.list2Str(roleIds));
		session.setAttribute(SSOConstants.USER_ROLES_S, EasyUtils.list2Str(roleNames));
    }
   
    public void execute() {
        Long logonUserId = Environment.getUserId();
        Long start = System.currentTimeMillis();
        
        Set<Long> roleIds = new LinkedHashSet<>();
        Set<String> roleNames = new LinkedHashSet<>();
        		
        HttpSession session = loadRights(logonUserId, roleIds, roleNames);
        loadGroups(logonUserId, session, roleIds, roleNames);
    	
    	// 记录登陆成功的日志信息
    	Object loginMsg = session.getAttribute(SSOConstants.LOGIN_MSG);
    	if( !Environment.isAnonymous() && loginMsg != null ) {
    		session.removeAttribute(SSOConstants.LOGIN_MSG); // remove，以免refresh userHas时反复记录登录日志
        	BusinessLogger.log("用户登录", Environment.getUserName(), loginMsg, null, start);
    	}
    }
}
