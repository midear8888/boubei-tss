package com.boubei.tss.modules.cloud.pay;

import com.boubei.tss.modules.cloud.entity.Account;


/**
 * @author hank 充值成功后续操作
 */
public class RechargeOrderHandler extends AbstractProduct {
	
	public String getName() {
		return PRODUCT_RECHARGE;
	}

	protected void handle() {
		Account account = getAccount();
		createIncomeFlow( account );
		
		commonDao.update(account);
	}
}
