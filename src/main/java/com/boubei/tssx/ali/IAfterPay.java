package com.boubei.tssx.ali;

import java.util.Map;

public interface IAfterPay {
	Object handle(Map<?, ?> trade_map, String payType);
}
