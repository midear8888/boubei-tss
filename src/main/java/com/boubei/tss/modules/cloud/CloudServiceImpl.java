/* ==================================================================   
T * Created [2016-06-22] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.cloud;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.EX;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.ICommonDao;
import com.boubei.tss.framework.sms.AbstractSMS;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.api.APIService;
import com.boubei.tss.modules.cloud.entity.CloudOrder;
import com.boubei.tss.modules.cloud.entity.ModuleDef;
import com.boubei.tss.modules.cloud.entity.ModuleUser;
import com.boubei.tss.modules.cloud.pay.AbstractProduct;
import com.boubei.tss.modules.cloud.pay.AfterPayService;
import com.boubei.tss.modules.cloud.pay.ModuleOrderHandler;
import com.boubei.tss.um.entity.RoleUser;
import com.boubei.tss.um.entity.SubAuthorize;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.service.IUserService;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;

@Service("CloudService")
public class CloudServiceImpl implements CloudService, AfterPayService{
	
	@Autowired ICommonDao commonDao;
	@Autowired IUserService userService;
	@Autowired APIService apiService;
	
	protected Logger log = Logger.getLogger(this.getClass());
		
/************************************* cloud module **************************************/
	
	public void selectModule(Long user, Long module) {
		checkIsDomainAdmin();
		ModuleDef def = (ModuleDef) commonDao.getEntity(ModuleDef.class, module);
		
		// 创建 ModuleUser映射关系
		ModuleUser mu = new ModuleUser(user, module);
		mu.setDomain( Environment.getDomainOrign() );
		
		commonDao.create(mu);
		
		// 生成一个转授策略
		SubAuthorize sa = new SubAuthorize();
		sa.setName(def.getId() + "_" + def.getModule() + "_test"); // name: 模块ID_模块名称_购买序号
		sa.setStartDate(new Date());
		sa.setOwnerId( Environment.getUserId() );
		
		Calendar calendar = new GregorianCalendar();
        Object try_days = EasyUtils.checkNull(def.getTry_days(), 31);
		calendar.add(Calendar.DAY_OF_YEAR, EasyUtils.obj2Int(try_days));
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
	
	
/************************************* cloud order **************************************/
	
	public CloudOrder createOrder(CloudOrder co) {
		if( Environment.isAnonymous() ) {
			selfRegister(co);
		}
		
		co.setCreator(Environment.getUserCode());
		
		if( EasyUtils.isNullOrEmpty(co.getType()) ){
			co.setType(ModuleOrderHandler.class.getName());
		}
		
		AbstractProduct product = AbstractProduct.createBean(co);
				
		product.beforeOrder(co);
		
		co.setProduct(product.getName());
		co.setStatus(CloudOrder.NEW);
		co.setOrder_date(new Date());
		co = (CloudOrder) commonDao.create(co);
		co.setOrder_no(co.getOrder_date().getTime() + "-" + co.getId());
		
		if(co.getModule_id() != null) {
			calMoney(co); // 价格以后台计算为准，防止篡改（同时检查前后台的报价是否一致）
		}
		
		return co;
	}
	
	@SuppressWarnings("unchecked")
	private void selfRegister(CloudOrder mo) {
		Map<String,String> map = new HashMap<>();
		try {  
  			map = new ObjectMapper().readValue(mo.getParams(), HashMap.class);
		} 
		catch (Exception e) { } 
		
		// 校验短信验证码smsCode
    	String smsCode = map.get("smsCode");
    	String mobile  = map.get("phone");
    	if( EasyUtils.isNullOrEmpty(smsCode) || !AbstractSMS.create().checkCode(mobile, smsCode) ) {
    		throw new BusinessException("短信验证码校验失败，请重新输入。");
    	}
        
        // 注册账号
        User user = new User();
        user.setLoginName(mobile);
        user.setTelephone(mobile);
        user.setBelongUserId(mo.getInvite_user_id());
        user.setUserName(mobile);
        user.setOrignPassword(map.get("password"));
        try {
        	userService.regUser(user, true);
        } 
        catch(Exception e) {
        	// 手机号已经注册过了
        	user = userService.getUserByLoginName(mobile);
        }
        
        user.setUserName( (String) EasyUtils.checkNull( map.get("user_name"), user.getUserName() ) );
    	user.setUdf( (String) EasyUtils.checkNull(map.get("company_name"), user.getUdf() ));
        
        // 模拟登录
        apiService.mockLogin(user.getLoginName());
	}
	
	public CloudOrder calMoney(CloudOrder mo) {
		ModuleDef md = (ModuleDef) commonDao.getEntity(ModuleDef.class, mo.getModule_id());
		
		mo.setPrice( md.getPrice() );
		
		Map<String, Object> params = BeanUtil.getProperties(mo);
		Double money = EasyUtils.eval(md.getPrice_def(), params);
		
		if(mo.getRebate() != null ) {
			money *= mo.getRebate();
		}
		if(mo.getDerate() != null) {
			money -= mo.getDerate();
		}
		
		mo.setMoney_cal( (double)Math.round(money*100) / 100 );
		
		return mo;
	}

	public void handle( String order_no, Double real_money, String payer,String payType, Map<?, ?> trade_map) {
		AbstractProduct iAfterPay = AbstractProduct.createBean(order_no);
		iAfterPay.afterPay(trade_map, real_money, payer, payType);
	}
	
	public void setSubAuthorizeRoles(Long userId, String roleIds, Long strategyId) {
		SubAuthorize sa = (SubAuthorize) commonDao.getEntity(SubAuthorize.class, strategyId);
		sa.setOwnerId(userId);
		commonDao.update(sa);
		
		List<?> roleIDList = Arrays.asList(roleIds.split(","));
			
		@SuppressWarnings("unchecked")
		List<RoleUser> rus = (List<RoleUser>) commonDao.getEntities(" from RoleUser where strategyId = ?", strategyId);
		for (RoleUser ru : rus) {
			if (roleIDList.contains(ru.getRoleId().toString())) {
				ru.setUserId(userId);
			} else {
				ru.setUserId(sa.getBuyerId());
			}
			commonDao.update(ru);
		}
	}

}
