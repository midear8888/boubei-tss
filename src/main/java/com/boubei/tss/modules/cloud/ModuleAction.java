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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.dm.record.Record;
import com.boubei.tss.dm.report.Report;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.modules.cloud.entity.ModuleDef;
import com.boubei.tss.util.EasyUtils;

@Controller
@RequestMapping("/auth/module")
public class ModuleAction {
	
	@Autowired private ModuleService service;
	@Autowired private ICommonService commonService;
	
	@RequestMapping(value = "/resources/{resource}")
	@ResponseBody
	public Object listResource(@PathVariable String resource) {
		String permissionTable = resource.replace("_", "_permission_");
		String sql = "select distinct t.id as value, t.name as text, t.decode, t.type " +
				" from " + resource + " t, " + permissionTable + " p, um_roleusermapping ru  " +
				" where t.id = p.resourceId and p.roleId = ru.roleId and ru.userId = ? " +
				"  and disabled = 0 and name not like '$%' and t.id >= 8 order by t.decode";
		
		List<Map<String, Object>> list = SQLExcutor.queryL(sql, Environment.getUserId());
		for( Map<String, Object> row : list ) {
			String text = row.get("text") + "（" +row.get("type")+ "）"; 
			row.put("text", text.replace("（0）", "（组）").replace("（1）", ""));
		}
		return list;
	}
	
	/**
	 * 当前用户所在域选用的模块里含有的资源列表（报表、录入表），用以过滤资源菜单
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/limitresources")
	@ResponseBody
	public Object[] limitResource() {
		String domain = Environment.getDomainOrign();
		if( EasyUtils.isNullOrEmpty(domain) ) { // 非域账号无需判断
			return new Object[] {};
		}
		
		Set<Long> reports = new LinkedHashSet<Long>();
		Set<Long> records = new LinkedHashSet<Long>();
		
//		String hql = "from ModuleDef where id in (select moduleId from ModuleUser where domain = ?)";
		String hql = "from ModuleDef where id in (select moduleId from ModuleUser where domain = ? " +
				" or userId in (" +Environment.getInSession(SSOConstants.USERIDS_OF_DOMAIN)+ ") )";
		
		List<ModuleDef> list = (List<ModuleDef>) commonService.getList(hql, domain);
		for(ModuleDef md : list) {
			String roles = md.getRoles();
			
			// 查出模块 roles 拥有查看权限的所有报表、录入表，如果模块没有单独指定报表、录入表列表，则取roles所有
			if( md.reports().isEmpty() ) {
				hql = "select distinct p.resourceId from ReportPermission p  where p.operationId = ? and p.roleId in (" +roles+ ") "; 
				List<Long> resourceIds = (List<Long>) commonService.getList( hql, Report.OPERATION_VIEW);
				reports.addAll( resourceIds );
			} else {
				reports.addAll( md.reports() );
			}
			
			if( md.records().isEmpty() ) {
				hql = "select distinct p.resourceId from RecordPermission p  where p.operationId = ? and p.roleId in (" +roles+ ") "; 
				List<Long> resourceIds = (List<Long>) commonService.getList( hql, Record.OPERATION_CDATA);
				resourceIds.addAll( (List<Long>) commonService.getList( hql, Record.OPERATION_VDATA) );
				records.addAll( resourceIds );
			} else {
				records.addAll( md.records() );
			}
		}
	        
		return new Object[] { reports, records };
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
