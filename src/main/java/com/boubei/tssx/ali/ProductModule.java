package com.boubei.tssx.ali;

import java.util.Map;

import com.boubei.tss.framework.Global;
import com.boubei.tss.modules.cloud.ModuleService;
import com.boubei.tss.util.EasyUtils;

public class ProductModule implements IAfterPay {

	public void handle(Map<?, ?> trade_map) {

		String orderNo = trade_map.get("out_trade_no").toString();

		String module_order_id = orderNo.split("-")[2];

		ModuleService ms = (ModuleService) Global.getBean("ModuleService");
		ms.payOrder(EasyUtils.obj2Long(module_order_id));
	}

}
