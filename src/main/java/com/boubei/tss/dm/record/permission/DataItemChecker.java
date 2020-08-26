package com.boubei.tss.dm.record.permission;

import java.util.Map;

public interface DataItemChecker {
	
	Long queryItemId( Long recordId, Map<String, ?> params );
	
	void editable( Long recordId, Long itemId, Map<String,?> params );
	
	void visiable( Long recordId, Long itemId, Map<String,?> params );

}
