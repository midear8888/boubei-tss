package com.boubei.tss.modules.cloud.product;

import com.boubei.tss.modules.cloud.entity.Account;
import com.boubei.tss.modules.cloud.entity.CloudOrder;

/**
 * @author hank 充值成功后续操作
 */
public class RechargeOrderHandler extends AbstractAfterPay {

	public RechargeOrderHandler(CloudOrder co) {
		super(co);
	}

	public Boolean handle() {
		// 获取账户
		Account account = getAccount();
		// 创建流水
		createIncomeFlow(account);
		return true;
	}
}
