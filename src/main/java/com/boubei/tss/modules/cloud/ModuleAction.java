/* ==================================================================   
 * Created [2016-06-22] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.cloud;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.util.EasyUtils;

@Controller
@RequestMapping("/auth/module")
public class ModuleAction {
	
	@Autowired private CloudService service;
	
	@RequestMapping(value = "/resources/{resource}")
	@ResponseBody
	public List<Map<String, Object>> listResource(@PathVariable String resource) {
		String permissionTable = resource.replace("_", "_permission_");
		String sql = "select distinct t.id as value, t.name as text, t.decode, t.type " +
				" from " + resource + " t, " + permissionTable + " p, um_roleusermapping ru  " +
				" where t.id = p.resourceId and p.roleId = ru.roleId and ru.userId = ? " +
				"  and disabled = 0 and name not like '$%' order by t.decode";
		
		List<Map<String, Object>> list = SQLExcutor.queryL(sql, Environment.getUserId());
		for( Map<String, Object> row : list ) {
			String text = "(" +row.get("type")+ ")" + row.get("text") + "（" +row.get("type")+ "）"; 
			text = text.replace("（0）", "（组）").replace("（1）", "").replace("（2）", "");
			text = text.replace("(0)", "").replace("(1)", "").replace("(2)", "-    ");
			
			row.put("text", text);
			row.remove("decode");
			row.remove("type");
		}
		return list;
	}
	
	@RequestMapping(value = "/allresources")
	@ResponseBody
	public List<Map<String, Object>> listAllResource() {
		List<Map<String, Object>> records = this.listResource("dm_record");
		for( Map<String, Object> row : records ) {
			row.put("value", "rc_" + row.get("value"));
		}
		
		List<Map<String, Object>> reports = this.listResource("dm_report");
		for( Map<String, Object> row : reports ) {
			row.put("value", "rp_" + row.get("value"));
		}
		records.addAll(reports);
		
		List<Map<String, Object>> menus = this.listResource("portal_navigator");
		for( Map<String, Object> row : menus ) {
			row.put("value", "menu_" + row.get("value"));
		}
		records.addAll(menus);
		
		return records;
	}
	
	/**
	 * 当前用户所在域选用的模块里含有的资源列表（报表、录入表），用以过滤资源菜单
	 */
	@RequestMapping(value = "/limitresources")
	@ResponseBody
	public Object[] limitResource() {
		String domain = Environment.getDomainOrign();
		if( EasyUtils.isNullOrEmpty(domain) ) { // 非域账号无需判断
			return new Object[] {};
		}

		return new Object[] { service.limitReports(), service.limitRecords(), service.limitMenus() };
	}
	
	@RequestMapping(value = "/{module}", method = RequestMethod.POST)
	@ResponseBody
	public Object selectModule(@PathVariable Long module) {
		service.selectModule(Environment.getUserId(), module);
		return "Success";
	}
	
	@RequestMapping(value = "/{module}", method = RequestMethod.DELETE)
	@ResponseBody
	public Object unSelectModule(@PathVariable Long module) {
		service.unSelectModule(Environment.getUserId(), module);
		return "Success";
	}
	
	/**
	 * 域用户选择模块后，获得了模块所含的角色；当模块新添加了角色后，自动刷给域用户
		afterListener = function(itemId) {
		    $.post("/tss/auth/module/refresh/" + itemId, {});
		}
	 */
	@RequestMapping(value = "/refresh/{module}", method = RequestMethod.POST)
	@ResponseBody
	public Object refreshModuleUserRoles(@PathVariable Long module) {
		service.refreshModuleUserRoles(module);
		return "Success";
	}
	
	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public List<?> listAvaliableModules() {
		return service.listAvaliableModules();
	}
	
	@RequestMapping(value = "/my", method = RequestMethod.GET)
	@ResponseBody
	public List<?> listSelectedModules() {
		Long userId = Environment.getUserId();
		return service.listSelectedModules(userId);
	}
}
