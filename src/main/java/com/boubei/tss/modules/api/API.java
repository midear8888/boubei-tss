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
import com.boubei.tss.modules.log.BusinessLogger;
import com.boubei.tss.portal.helper.MenuDTO;
import com.boubei.tss.portal.service.INavigatorService;
import com.boubei.tss.um.service.ILoginService;

/**
 * 远程调用接口。
 * GeneralSearchAction 无法被远程访问
 */
@Controller
@RequestMapping( {"/api", "/auth/api"} )
public class API {
	
	@Autowired ILoginService loginService;
	@Autowired INavigatorService menuService;
	
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
		BusinessLogger.log(table, code, content, udf1,System.currentTimeMillis());
	}
	
}
