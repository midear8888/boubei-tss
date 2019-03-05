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
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.entity.RoleUser;
import com.boubei.tss.um.entity.SubAuthorize;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.service.IUserService;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;

@Service("CloudService")
public class CloudServiceImpl implements CloudService, AfterPayService {

	@Autowired
	ICommonDao commonDao;
	@Autowired
	IUserService userService;
	@Autowired
	APIService apiService;

	protected Logger log = Logger.getLogger(this.getClass());

	/************************************* cloud module **************************************/

	public void selectModule(Long user, Long module) {
		checkIsDomainAdmin();
		ModuleDef def = (ModuleDef) commonDao.getEntity(ModuleDef.class, module);

		// 创建 ModuleUser映射关系
		ModuleUser mu = new ModuleUser(user, module);
		mu.setDomain(Environment.getDomainOrign());

		commonDao.create(mu);

		// 生成一个转授策略
		SubAuthorize sa = new SubAuthorize();
		sa.setName(def.getId() + "_" + def.getModule() + "_" + user); // name:
																	// 模块ID_模块名称_购买人_购买序号
		sa.setStartDate(new Date());
		sa.setOwnerId(Environment.getUserId());
		sa.setBuyerId(Environment.getUserId());

		Calendar calendar = new GregorianCalendar();
		Object try_days = EasyUtils.checkNull(def.getTry_days(), 31);
		calendar.add(Calendar.DAY_OF_YEAR, EasyUtils.obj2Int(try_days));
		sa.setEndDate(calendar.getTime());
		commonDao.create(sa);

		// 设置转授权限给当前域管理员
		for (Long roleId : def.roles()) {
			RoleUser ru = new RoleUser();
			ru.setRoleId(roleId);
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
		if (!Environment.isDomainUser()) {
			throw new BusinessException(EX.MODULE_1);
		}
	}

	/**
	 * 域用户选择模块后，获得了模块所含的角色；当模块新添加了角色后，自动刷给域用户。
	 * 避免域用户需要重新选择模块才能获取新角色（先【结束试用】，再【我要试用】）
	 * 注：模块角色减少时，本方法只能去掉域管理员的角色；域管理员也已经把角色授给了其它域成员的话，则无法收回
	 */
	@SuppressWarnings("unchecked")
	public void refreshModuleUserRoles(Long module) {
		// 模块被多少域用户购买使用
		List<Long> userIds = (List<Long>) commonDao.getEntities("select userId from ModuleUser where moduleId = ?", module);
		List<RoleUser> ruList = (List<RoleUser>) commonDao.getEntities("from RoleUser where moduleId = ?", module);
		String hql3 = "select id from SubAuthorize where name like ? and buyerId = ?";

		ModuleDef def = (ModuleDef) commonDao.getEntity(ModuleDef.class, module);
		for (Long roleId : def.roles()) {
			for (Long domainUserId : userIds) {

				// 当前域用户已经获得的模块策略
				List<Long> strategyIds = (List<Long>) commonDao.getEntities(hql3, module + "_%", domainUserId);
				for (Long strategyId : strategyIds) {
					RoleUser ru = new RoleUser();
					ru.setRoleId(roleId);
					ru.setUserId(domainUserId);
					ru.setModuleId(module);
					ru.setStrategyId(strategyId);

					if (!ruList.contains(ru)) {
						commonDao.create(ru);
					}
				}
			}
		}
	}

	public void unSelectModule(Long user, Long module) {
		checkIsDomainAdmin();

		commonDao.deleteAll(commonDao.getEntities("from RoleUser where userId=? and moduleId=?", user, module));
		commonDao.deleteAll(commonDao.getEntities("from ModuleUser where userId=? and moduleId=?", user, module));
	}

	public List<?> listSelectedModules(Long user) {
		String hql = "select o from ModuleDef o, ModuleUser mu " + " where mu.moduleId = o.id and mu.userId = ? and o.status in ('opened', 'closed')"
				+ " order by o.id desc ";
		return commonDao.getEntities(hql, user);
	}

	public List<?> listAvaliableModules() {
		String hql = "from ModuleDef o where o.status in ('opened') order by o.seqno asc, o.id desc ";
		return commonDao.getEntities(hql);
	}

	/************************************* cloud order **************************************/

	@SuppressWarnings("unchecked")
	public CloudOrder createOrder(CloudOrder co) {
		Map<String, String> map = new HashMap<>();
		try { map = new ObjectMapper().readValue(co.getParams(), HashMap.class); } catch (Exception e) { }

		Boolean isNewUser = false;
		// 自注册账号
		if (Environment.isAnonymous()) {
			isNewUser = selfRegister(co, map);
		}

		// for 购买时补充填写用户信息
		String phone    = map.get("phone");
		String userName = map.get("user_name");
		String orgName  = map.get("company_name");
		
		String userCode = (String) EasyUtils.checkNull(phone, Environment.getUserCode());
		User user = userService.getUserByLoginName(userCode);
		user.setUserName((String) EasyUtils.checkNull(userName, user.getUserName()));
		user.setUdf((String) EasyUtils.checkNull(orgName, user.getUdf()));
		user.setDisabled(ParamConstants.FALSE); // 第一次下单没有支付，账号在自注册域，状态停用；第二次再重新下单先启用
		commonDao.update(user);
		
		// 模拟登录
		if(Environment.isAnonymous()){
			apiService.mockLogin(userCode);
		}

		co.setCreator(userCode);
		co.setType( (String) EasyUtils.checkNull(co.getType(), ModuleOrderHandler.class.getName()) );
		AbstractProduct product = AbstractProduct.createBean(co);
		product.beforeOrder();

		co.setProduct(product.getName());
		co.setStatus(CloudOrder.NEW);
		co.setOrder_date(new Date());
		co = (CloudOrder) commonDao.create(co);
		
		co.setOrder_no(co.getOrder_date().getTime() + "-" + co.getId());
		if (co.getModule_id() != null) {
			calMoney(co); // 价格以后台计算为准，防止篡改（同时检查前后台的报价是否一致）
		}
		
		// 如果是下单生成的自注册新用户，则先停用，支付后启用
		if( isNewUser || UMConstants.SELF_REGISTER_GROUP_ID.equals(Environment.getUserGroupId()) ){
			user.setDisabled(ParamConstants.TRUE);
			commonDao.update(user);
		}

		return co;
	}

	private Boolean selfRegister(CloudOrder mo, Map<String, String> map) {
		// 校验短信验证码smsCode
		String smsCode = map.get("smsCode");
		String mobile = map.get("phone");
		if (EasyUtils.isNullOrEmpty(smsCode) || !AbstractSMS.create().checkCode(mobile, smsCode)) {
			throw new BusinessException("短信验证码校验失败，请重新输入。");
		}

		List<?> users = commonDao.getEntities(" from User where ? in (loginName, telephone, email)", mobile);

		if (users.size() > 0) {
			return false;
		}

		// 注册账号
		User user = new User();
		user.setLoginName(mobile);
		user.setTelephone(mobile);
		user.setBelongUserId(mo.getInvite_user_id());
		user.setUserName(mobile);
		user.setPassword(map.get("password"));
		userService.regUser(user, true);
		return true;
	}

	public CloudOrder calMoney(CloudOrder co) {
		ModuleDef md = (ModuleDef) commonDao.getEntity(ModuleDef.class, co.getModule_id());

		co.setPrice(md.getPrice());

		Map<String, Object> params = BeanUtil.getProperties(co);
		Double money = EasyUtils.eval(md.getPrice_def(), params);

		if (co.getRebate() != null) {
			money *= co.getRebate();
		}
		if (co.getDerate() != null) {
			money -= co.getDerate();
		}

		co.setMoney_cal((double) Math.round(money * 100) / 100);

		return co;
	}

	public void handle(String order_no, Double real_money, String payer, String payType, Map<?, ?> trade_map) {
		AbstractProduct iAfterPay = AbstractProduct.createBean(order_no);
		iAfterPay.afterPay(trade_map, real_money, payer, payType);
	}

	public void setSubAuthorizeRoles(Long userId, String ruIds, Long strategyId) {
		SubAuthorize sa = (SubAuthorize) commonDao.getEntity(SubAuthorize.class, strategyId);
		sa.setOwnerId(userId);
		commonDao.update(sa);

		List<?> ruIDList = Arrays.asList(ruIds.split(","));

		@SuppressWarnings("unchecked")
		List<RoleUser> rus = (List<RoleUser>) commonDao.getEntities(" from RoleUser where strategyId = ?", strategyId);
		for (RoleUser ru : rus) {
			if (ruIDList.contains(ru.getId().toString())) {
				ru.setUserId(userId);
			} else {
				ru.setUserId(sa.getBuyerId());
			}
			commonDao.update(ru);
		}
	}

}
