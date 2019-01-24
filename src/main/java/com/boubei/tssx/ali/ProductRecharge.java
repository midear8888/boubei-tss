package com.boubei.tssx.ali;

import java.util.Date;
import java.util.List;

import com.boubei.tss.modules.cloud.entity.Account;
import com.boubei.tss.modules.cloud.entity.AccountFlow;
import com.boubei.tss.modules.cloud.entity.CloudOrder;
import com.boubei.tss.modules.sn.SerialNOer;

public class ProductRecharge extends AbstractAfterPay {

	public ProductRecharge(CloudOrder co) {
		super(co);
	}

	public Boolean handle() {
		// 获取账户
		@SuppressWarnings("unchecked")
		List<Account> accounts = (List<Account>) commonDao.getEntities(" from Account where belong_user_id = ?", userId);
		Account account;
		if (accounts.size() > 0) {
			account = accounts.get(0);
		} else {
			account = new Account();
			account.setBalance(0D);
			account.setBelong_user_id(userId);
			account.setDomain(co.getDomain());
			account = (Account) commonDao.create(account);
		}
		Long account_id = account.getId();
		// 创建流水
		AccountFlow flow = new AccountFlow();
		flow.setAccount_id(account_id);
		flow.setMoney(co.getMoney_real());
		flow.setOrder_no(co.getOrder_no());
		flow.setPay_man(this.trade_map.get("buyer_id").toString());
		flow.setPay_time(new Date());
		flow.setPayment(this.payType);
		flow.setSn(SerialNOer.get("AF"));
		flow.setType(co.getType());
		Double balance = account.getBalance() + flow.getMoney();
		flow.setBalance(balance);
		commonDao.create(flow);
		return true;
	}
}
