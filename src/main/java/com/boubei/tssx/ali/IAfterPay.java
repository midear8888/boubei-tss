package com.boubei.tssx.ali;

import java.util.Map;

public interface IAfterPay {
	void handle(Map<?, ?> trade_map);
}
