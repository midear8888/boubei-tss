package com.boubei.tss.dm.record;

import java.util.Map;

import com.boubei.tss.dm.ddl._Database;
import com.boubei.tss.dm.dml.SQLExcutor;

/**
 * 录入表自定义事件
 * 
 * rcEventClass:=com.boubei.xx.xx
 * 
 */
public interface RecordEvent {
	
	void beforeSelect(Map<String, String> params, _Database db);
	
	void afterSelect(SQLExcutor ex, _Database db);
	
	void beforeInsert(Map<String, String> values);
	
	void afterInsert(Long id, Map<String, String> values, _Database db);
	
	 Map<String, Object> beforeUpdate(Long id, Map<String, String> values, _Database db);
	
	void afterUpdate(Long id, Map<String, Object> item, _Database db);
	
	 Map<String, Object> beforeDelete(Long id, _Database db);
	
	void afterDelete(Long id, Map<String, Object> item, _Database db);

	void afterLogicDelete(Long id, Map<String, Object> old, _Database db);

}
