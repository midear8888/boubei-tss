package com.boubei.tss.modules.cloud.pay;

import java.util.Map;

public interface AfterPayService {
	
	/**
	 * @param order_no 商户订单号 CloudOrder.order_no
	 * @param real_money 真实付款金额
	 * @param payer 
	 * @param payType 支付类型
	 * @param map
	 * @return
	 */
	void handle(String order_no, Double real_money, String payer, String payType, Map<?, ?> map);
}
