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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.EX;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.ICommonDao;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.um.entity.RoleUser;
import com.boubei.tss.util.EasyUtils;

@Service("ModuleService")
public class ModuleServiceImpl implements ModuleService {
	
	@Autowired ICommonDao commonDao;
	
	public void selectModule(Long user, Long module) {
		checkIsDomainAdmin();
		
		ModuleUser mu = new ModuleUser(user, module);
		commonDao.create(mu);
		
		ModuleDef def = (ModuleDef) commonDao.getEntity(ModuleDef.class, module);
		String[] roles = def.getRoles().split(",");
		for(String role : roles) {
			RoleUser ru  = new RoleUser();
			Long roleId = EasyUtils.obj2Long(role);
			ru.setRoleId( roleId );
			ru.setUserId(user);
			commonDao.create(ru);
		}
	}
	
	/**
	 * 检查当前用户是否为域管理员，只有域管理员可以选择或取消功能模块
	 */
	private void checkIsDomainAdmin() {
		if( !Environment.isDomainUser() ) {
			throw new BusinessException(EX.MODULE_1);
		}
	}
	
	/**
	 * 域用户选择模块后，获得了模块所含的角色；当模块新添加了角色后，自动刷给域用户。
	 * 避免域用户需要重新选择模块才能获取新角色（先结束试用，再 我要试用）
	 */
	public void refreshModuleUserRoles( Long module ) {
		List<?> domainUserIds = commonDao.getEntities("select userId from ModuleUser where moduleId = ?", module);
		ModuleDef def = (ModuleDef) commonDao.getEntity(ModuleDef.class, module);
		String[] roles = def.getRoles().split(",");
		for(String role : roles) {
			Long roleId = EasyUtils.obj2Long(role);
			for( Object obj : domainUserIds ) {
				Long userId = EasyUtils.obj2Long(obj);
				List<?> temp = commonDao.getEntities("from RoleUser where roleId = ? and userId = ?", roleId, userId);
				if( temp.isEmpty() ) {
					RoleUser ru  = new RoleUser();
					ru.setRoleId( roleId );
					ru.setUserId( userId );
					commonDao.create(ru);
				}
			}
		}
	}
	
	public void unSelectModule( Long user, Long module ) {
		checkIsDomainAdmin();
		
		ModuleDef def = (ModuleDef) commonDao.getEntity(ModuleDef.class, module);
		String[] roles = def.getRoles().split(",");
		for(String role : roles) {
			commonDao.deleteAll( commonDao.getEntities("from RoleUser where roleId=? and userId=?", EasyUtils.obj2Long(role), user) );
		}
		commonDao.deleteAll( commonDao.getEntities("from ModuleUser where userId=? and moduleId=?", user, module) );
	}
	
	public List<?> listSelectedModules(Long user) {
		String hql = "select o from ModuleDef o, ModuleUser mu " +
				" where mu.moduleId = o.id and mu.userId = ? and o.status in ('opened', 'closed')" +
				" order by o.id desc ";
		return commonDao.getEntities(hql, user);
	}

	public List<?> listAvaliableModules() {
		String hql = "select o from ModuleDef o where o.status in ('opened') order by o.id desc ";
		return commonDao.getEntities(hql);
	}
}
