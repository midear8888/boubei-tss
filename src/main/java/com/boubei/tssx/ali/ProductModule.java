package com.boubei.tssx.ali;

import java.util.Map;

import com.boubei.tss.framework.Global;
import com.boubei.tss.modules.cloud.ModuleService;
import com.boubei.tss.util.EasyUtils;

public class ProductModule implements IAfterPay {

	@SuppressWarnings("unchecked")
	public void handle(Object params) {
		Map<String, Object> trade_map = (Map<String, Object>) params;
		String orderNo = trade_map.get("out_trade_no").toString();
		// String module_id = orderNo.split("-")[1];
		String module_order_id = orderNo.split("-")[2];

		ModuleService ms = (ModuleService) Global.getBean("ModuleService");
		ms.payOrder(EasyUtils.obj2Long(module_order_id));

		System.out.println("ProductModule done!");
	}

}
