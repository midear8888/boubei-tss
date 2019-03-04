package com.boubei.tss.modules.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.modules.log.BusinessLogger;
import com.boubei.tss.portal.helper.MenuDTO;
import com.boubei.tss.portal.service.INavigatorService;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.um.service.IUserService;
import com.boubei.tss.util.EasyUtils;

/**
 * 远程调用接口。
 * GeneralSearchAction 无法被远程访问
 */
@Controller
@RequestMapping( {"/api", "/auth/api"} )
public class API {
	
	@Autowired ILoginService loginService;
	@Autowired INavigatorService menuService;
	@Autowired IUserService userService;
	@Autowired ICommonService commService;
	
	@RequestMapping("/menu/json/{id}")
	@ResponseBody
    public List<MenuDTO> menuJSON(@PathVariable("id") Long id) {
        return menuService.getMenuTree(id);
    }
	
	/**
	 * http://localhost:9000/tss/api/roles
	 * 
	 * 系统角色列表，用于解析角色名对角色ID。 
	 * 例：staff_info.position记录的是角色ID，判断是否包含某角色时不宜写死角色ID，通过名称先去找出角色ID
	 * {"业务员":73, 73:"业务员","人事经理":69,69:"人事经理"....}
	 */
	@RequestMapping(value = "/roles")
	@ResponseBody
	public Map<Object, Object> getRoles() {
		Map<Object, Object> roleMap = new HashMap<Object, Object>();
		
		String sql = "select id, name from um_role where isGroup=0 and disabled=0 and id > 0 ";
		List<Map<String, Object>> list = SQLExcutor.queryL(sql);
		for( Map<String, Object> m : list ) {
			Object roleName = m.get("name");
			Object roleId = m.get("id");
			
			roleMap.put(roleName, roleId);
			roleMap.put(roleId, roleName);
		}
		return roleMap;
	}
	
	/**
	 * http://localhost:9000/tss/api/users
	 * 登陆账号和中文名字映射
	 */
	@RequestMapping(value = "/users")
	@ResponseBody
	public Map<String, String> getUsers() {
		return loginService.getUsersMap();
	}
	
	/**
	 * http://localhost:9000/tss/api/log
	 * 记录日志
	 */
	@RequestMapping(value = "/log", method = RequestMethod.POST)
	@ResponseBody
	public void createLog(HttpServletRequest request) {
		Map<String, String> requestMap = DMUtil.parseRequestParams(request, false);
		String table = requestMap.get("table");
		String code = requestMap.get("code");
		String content = requestMap.get("content");
		String udf1 = requestMap.get("udf1");
		BusinessLogger.log(table, code, content, udf1, System.currentTimeMillis());
	}
	
	/** 指定人员特定的岗位, 用于特定的页面添加账号及角色：客户管理、司机管理等 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/setUserRole", method = RequestMethod.POST)
	@ResponseBody
	public User setRole4User(HttpServletRequest request, String userCode, String group, String roles) {
		Map<String, String> requestMap = DMUtil.parseRequestParams(request, false);
		
		User user = userService.getUserByLoginName(userCode);
		String groupStr;
		
		// 只能修改自己域下用户，修改别的域已存在的用户会报账号已存在
		if( user != null && loginService.getUsersMap().containsKey(userCode) ) {
			Long userID = user.getId();
			
			List<Long> exsitRoles = loginService.getRoleIdsByUserId(userID);
			List<Object[]> groups = loginService.getAssistGroups(userID);
			groups.add( loginService.getMainGroup(userID) );
			
			groupStr = EasyUtils.list2Str(groups, 0);
			roles += "," + EasyUtils.list2Str(exsitRoles);
		} 
		else {
			user = new User();
			user.setLoginName(userCode);
			user.setPassword(userCode);
			
	        String hql = "select id from Group where domain = ? and name = ? order by decode asc";
			List<Object> list = (List<Object>) commService.getList(hql, Environment.getDomain(), EasyUtils.checkNull(group, "noGroup__"));
			list.addAll( commService.getList(hql, Environment.getDomain(), Environment.getInSession(SSOConstants.USER_GROUP)) );
			
			groupStr = list.get(0) + ""; // 创建用户到当前创建人所在组下
		}
		
		user.setUserName(requestMap.get("userName"));
		user.setUdf(requestMap.get("udf"));
		userService.createOrUpdateUser(user, groupStr, roles);
		
		return user;
	}
}
