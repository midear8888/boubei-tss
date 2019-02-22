package com.boubei.tss.modules.cloud.pay;

import com.boubei.tss.modules.cloud.entity.Account;
import com.boubei.tss.modules.cloud.entity.AccountFlow;

/**
 * @author hank 充值成功后续操作
 */
public class RechargeOrderHandler extends AbstractProduct {

	public String getName(){
		return AccountFlow.TYPE1;
	}
	
	protected String toflowType() {
		return AccountFlow.TYPE1;
	}

	protected void handle() {
		Account account = getAccount();
		createIncomeFlow(account);

		commonDao.update(account);
	}
}
