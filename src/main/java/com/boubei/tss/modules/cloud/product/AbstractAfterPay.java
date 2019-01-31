package com.boubei.tss.modules.cloud.product;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.persistence.ICommonDao;
import com.boubei.tss.modules.api.APIService;
import com.boubei.tss.modules.cloud.ModuleService;
import com.boubei.tss.modules.cloud.entity.Account;
import com.boubei.tss.modules.cloud.entity.AccountFlow;
import com.boubei.tss.modules.cloud.entity.CloudOrder;
import com.boubei.tss.modules.cloud.entity.ModuleDef;
import com.boubei.tss.modules.sn.SerialNOer;
import com.boubei.tss.um.dao.IUserDao;
import com.boubei.tss.um.entity.RoleUser;
import com.boubei.tss.um.entity.SubAuthorize;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.service.IUserService;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MathUtil;

public abstract class AbstractAfterPay implements IAfterPay {

	protected static ICommonDao commonDao = (ICommonDao) Global.getBean("CommonDao");
	protected APIService apiService = (APIService) Global.getBean("APIService");
	protected IUserService userService = (IUserService) Global.getBean("UserService");
	protected IUserDao userDao = (IUserDao) Global.getBean("UserDao");
	protected ModuleService moduleService = (ModuleService) Global.getBean("ModuleService");

	static String path = AbstractAfterPay.class.getName().replace(AbstractAfterPay.class.getSimpleName(), "");

	public CloudOrder co;
	public Long userId;
	public String userCode;
	public Map<?, ?> trade_map;
	public String payType;
	public String payer;
	public User user;

	public AbstractAfterPay() {

	}

	public AbstractAfterPay(CloudOrder co) {
		this.co = co;
	}

	public static AbstractAfterPay createBean(String out_trade_no) {
		String cloudOrderId = out_trade_no.split("-")[1];
		if (EasyUtils.isNullOrEmpty(cloudOrderId)) {
			return null;
		}
		CloudOrder co = (CloudOrder) commonDao.getEntity(CloudOrder.class, EasyUtils.obj2Long(cloudOrderId));
		String clazz = co.getType().indexOf(".") > 0 ? "" : path;
		return (AbstractAfterPay) BeanUtil.newInstanceByName(clazz + co.getType(), new Class[] { CloudOrder.class }, new Object[] { co });
	}

	public Object handle(Map<?, ?> trade_map, Double real_money, String payer, String payType) {
		this.trade_map = trade_map;
		this.payType = payType;
		this.payer = payer;

		user = (User) commonDao.getEntities(" from User where loginName = ?", co.getCreator()).get(0);
		this.userId = user.getId();
		this.userCode = user.getLoginName();

		if (!CloudOrder.NEW.equals(co.getStatus())) {
			// throw new BusinessException("订单已支付");
			return "订单" + co.getStatus();
		}

		if (!real_money.equals(co.getMoney_cal())) {
			return "订单金额不符";
		}
		co.setMoney_real(real_money);
		co.setPay_date(new Date());
		co.setStatus(CloudOrder.PAYED);
		commonDao.update(co);
		if (handle()) {
			return "success";
		}
		return "false";
	}

	protected Account getAccount() {
		return getAccount(userId);
	}
	
	protected Account getAccount(Long userId) {
		@SuppressWarnings("unchecked")
		List<Account> accounts = (List<Account>) commonDao.getEntities(" from Account where belong_user_id = ?", userId);

		if (accounts.size() > 0) {
			return accounts.get(0);
		}

		Account account = new Account();
		account.setBalance(0D);
		account.setBelong_user_id(userId);
		account.setDomain(co.getDomain());
		account = (Account) commonDao.create(account);
		return account;
	}

	public abstract Boolean handle();

	protected void createFlows(Account account) {
		createIncomeFlow(account);
		createBuyFlow(account);
	}

	protected void createIncomeFlow(Account account) {
		// 创建充值流水
		AccountFlow flow = new AccountFlow();
		flow.setAccount_id(account.getId());
		flow.setMoney(co.getMoney_real());
		flow.setOrder_no(co.getOrder_no());
		flow.setPay_man(payer);
		flow.setPay_time(new Date());
		flow.setPayment(this.payType);
		flow.setSn(SerialNOer.get("AF"));
		flow.setType(CloudOrder.TYPE0);
		Double balance = MathUtil.addDoubles(account.getBalance(), flow.getMoney());
		flow.setBalance(balance);
		account.setBalance(balance);
		commonDao.create(flow);
	}

	protected void createBuyFlow(Account account) {
		// 创建扣款流水
		AccountFlow flow = new AccountFlow();
		flow.setAccount_id(account.getId());
		flow.setMoney(-co.getMoney_real());
		flow.setOrder_no(co.getOrder_no());
		flow.setPay_man(payer);
		flow.setPay_time(new Date());
		flow.setPayment(this.payType);
		flow.setSn(SerialNOer.get("AF"));
		flow.setType(co.getType());
		Double balance = MathUtil.addDoubles(account.getBalance(), flow.getMoney());
		flow.setBalance(balance);
		account.setBalance(balance);
		commonDao.create(flow);
	}

	protected void createSubAuthorize() {
		int account_num = co.getAccount_num();
		int mouth_num = co.getMonth_num();
		Long module_id = co.getModule_id();
		ModuleDef def = (ModuleDef) commonDao.getEntity(ModuleDef.class, module_id);
		for (int i = 0; i < account_num; i++) {
			SubAuthorize sa = new SubAuthorize();
			sa.setName(def.getId() + "_" + def.getModule() + "_" + i); // name=模块ID_模块名称_购买序号

			sa.setStartDate(new Date());
			sa.setOwnerId(userId);
			sa.setBuyerId(userId);

			Calendar calendar = new GregorianCalendar();
			calendar.add(Calendar.MONTH, mouth_num);
			sa.setEndDate(calendar.getTime());

			commonDao.create(sa);

			// 创建策略角色对应关系
			String[] roleIds = EasyUtils.checkNullI(def.getRoles(), "").toString().split(",");
			for (String roleId : roleIds) {
				RoleUser ru = new RoleUser();
				ru.setModuleId(module_id);
				ru.setRoleId(EasyUtils.obj2Long(roleId));
				ru.setStrategyId(sa.getId());
				ru.setUserId(userId);
				commonDao.create(ru);
			}

		}
	}

}
