package com.boubei.tss.modules.cloud;

import com.boubei.tss.modules.cloud.entity.CloudOrder;

public interface IBeforeOrderCheck {
	void vaild(CloudOrder co);
}
