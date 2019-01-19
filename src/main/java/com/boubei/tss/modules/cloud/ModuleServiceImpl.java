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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import com.boubei.tss.EX;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.ICommonDao;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.cloud.entity.ModuleDef;
import com.boubei.tss.modules.cloud.entity.ModuleOrder;
import com.boubei.tss.modules.cloud.entity.ModuleUser;
import com.boubei.tss.um.entity.RoleUser;
import com.boubei.tss.um.entity.SubAuthorize;
import com.boubei.tss.util.EasyUtils;

@Service("ModuleService")
public class ModuleServiceImpl implements ModuleService {
	
	@Autowired ICommonDao commonDao;
	
	public void selectModule(Long user, Long module) {
		checkIsDomainAdmin();
		ModuleDef def = (ModuleDef) commonDao.getEntity(ModuleDef.class, module);
		
		// 创建 ModuleUser映射关系
		ModuleUser mu = new ModuleUser(user, module);
		mu.setDomain( Environment.getDomainOrign() );
		
		commonDao.create(mu);
		
		// 生成一个转授策略
		SubAuthorize sa = new SubAuthorize();
		sa.setName(def.getModule() + "_" + user);
		sa.setStartDate(new Date());
		sa.setOwnerId( Environment.getUserId() );
		
		Calendar calendar = new GregorianCalendar();
        calendar.add(Calendar.DAY_OF_YEAR, EasyUtils.obj2Int(def.getTry_days()));
		sa.setEndDate(calendar.getTime());
		commonDao.create(sa);
		
		// 设置转授权限给当前域管理员
		for(Long roleId : def.roles()) {
			RoleUser ru  = new RoleUser();
			ru.setRoleId( roleId );
			ru.setUserId(user);
			ru.setStrategyId(sa.getId());
			ru.setModuleId(module);
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
	 * 避免域用户需要重新选择模块才能获取新角色（先【结束试用】，再【我要试用】）
	 * 注：模块角色减少时，本方法只能去掉域管理员的角色；域管理员也已经把角色授给了其它域成员的话，则无法收回
	 */
	public void refreshModuleUserRoles( Long module ) {
		// 先清除由当前模块产生的域用户对角色关系
		commonDao.deleteAll( commonDao.getEntities("from RoleUser where moduleId = ?", module) );
		
		List<?> domainUserIds = commonDao.getEntities("select userId from ModuleUser where moduleId = ?", module);
		
		ModuleDef def = (ModuleDef) commonDao.getEntity(ModuleDef.class, module);
		for(Long roleId : def.roles()) {
			for( Object obj : domainUserIds ) {
				Long domainUserId = EasyUtils.obj2Long(obj);
				
				RoleUser ru  = new RoleUser();
				ru.setRoleId( roleId );
				ru.setUserId( domainUserId );
				ru.setModuleId( module );
				commonDao.create(ru);
			}
		}
	}
	
	public void unSelectModule( Long user, Long module ) {
		checkIsDomainAdmin();
		
		commonDao.deleteAll( commonDao.getEntities("from RoleUser where userId=? and moduleId=?", user, module) );
		commonDao.deleteAll( commonDao.getEntities("from ModuleUser where userId=? and moduleId=?", user, module) );
	}
	
	public List<?> listSelectedModules(Long user) {
		String hql = "select o from ModuleDef o, ModuleUser mu " +
				" where mu.moduleId = o.id and mu.userId = ? and o.status in ('opened', 'closed')" +
				" order by o.id desc ";
		return commonDao.getEntities(hql, user);
	}

	public List<?> listAvaliableModules() {
		String hql = "select o from ModuleDef o where o.status in ('opened') order by o.seqno asc, o.id desc ";
		return commonDao.getEntities(hql);
	}
	
	public Object payOrder(@PathVariable Long id) {
		Long userId = Environment.getUserId();
		ModuleOrder mo = (ModuleOrder) commonDao.getEntity(ModuleOrder.class, id);
		if( ModuleOrder.PAYED.equals(mo.getStatus()) ){
			throw new BusinessException("订单已支付");
		}
		mo.setPay_date( new Date() );
		mo.setStatus(ModuleOrder.PAYED);
		commonDao.update(mo);
		
		int account_num = mo.getAccount_num();
		int mouth_num = mo.getMonth_num();
		for(int i = 0; i < account_num; i++) {
			SubAuthorize sa = new SubAuthorize();
			sa.setName( mo.getModule_id() + "_" + userId + "_" + i );
			sa.setStartDate(new Date());
			sa.setOwnerId( userId );
			
			Calendar calendar = new GregorianCalendar();
	        calendar.add(Calendar.MONTH, mouth_num);
			sa.setEndDate(calendar.getTime());
			
			commonDao.create(sa);
		}
		
		// 如果此时还没有选择 试用模块， 在此创建 ModuleUser 映射关系
		String domain = Environment.getDomain();
		String hql = "from ModuleUser where userId = ? and moduleId = ? and domain = ?";
		List<?> list = commonDao.getEntities(hql, Environment.getUserId(), mo.getModule_id(), domain);
		if( list.isEmpty() ) {
			ModuleUser mu = new ModuleUser(Environment.getUserId(), mo.getModule_id());
			mu.setDomain( domain );
			commonDao.create(mu);
		} 
		
		return "Success";
	}
}
