package com.boubei.tss.modules.cloud.product;

import java.util.Map;

public interface IAfterPay {
	Object handle(Map<?, ?> trade_map, Double real_money, String payer, String payType);
}
