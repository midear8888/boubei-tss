package com.boubei.tssx.ali;

import java.util.Map;

import com.boubei.tss.framework.Global;
import com.boubei.tss.modules.cloud.ModuleService;
import com.boubei.tss.util.EasyUtils;

public class ProductModule implements IAfterPay {

	public void handle(Map<?, ?> trade_map) {

		String orderNo = trade_map.get("out_trade_no").toString();
		Double receipt_amount = EasyUtils.obj2Double(trade_map.get("receipt_amount"));
		Long module_order_id = EasyUtils.obj2Long(orderNo.split("-")[2]);

		ModuleService ms = (ModuleService) Global.getBean("ModuleService");
		ms.payOrder(module_order_id, receipt_amount);
	}

}
