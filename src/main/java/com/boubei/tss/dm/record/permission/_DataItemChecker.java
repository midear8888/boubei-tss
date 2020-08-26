package com.boubei.tss.dm.record.permission;

import java.util.Map;

import com.boubei.tss.modules.param.ParamConfig;
import com.boubei.tss.util.BeanUtil;

public class _DataItemChecker implements DataItemChecker {

	public Long queryItemId(Long recordId, Map<String, ?> params) {
		return 0L;
	}

	public void editable(Long recordId, Long itemId, Map<String,?> params) {
		
	}

	public void visiable(Long recordId, Long itemId, Map<String,?> params) {
		
	}
	
	public static DataItemChecker getInstance() {
		String clazz = ParamConfig.getAttribute("DataItemCheckerClass", _DataItemChecker.class.getName());
		return (DataItemChecker) BeanUtil.newInstanceByName(clazz);
	}

}
