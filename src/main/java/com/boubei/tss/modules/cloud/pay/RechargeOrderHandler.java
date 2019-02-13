package com.boubei.tss.modules.cloud.pay;


/**
 * @author hank 充值成功后续操作
 */
public class RechargeOrderHandler extends AbstractProduct {
	
	@SuppressWarnings("static-access")
	public String getName() {
		return this.PRODUCT_RECHARGE;
	}

	protected void handle() {
		createIncomeFlow( getAccount() );
	}
}
