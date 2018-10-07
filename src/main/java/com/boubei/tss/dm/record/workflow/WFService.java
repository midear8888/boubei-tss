package com.boubei.tss.dm.record.workflow;

import java.util.List;
import java.util.Map;

import com.boubei.tss.dm.ddl._Database;
import com.boubei.tss.dm.dml.SQLExcutor;

public interface WFService {
	
	WFStatus getWFStatus(Long tableId, Long itemId);
	
	void removeWFStatus(Long tableId, Long itemId);
	
	Map<Object, Object> getMyWFCount();

	void appendWFInfo(_Database _db, Map<String, Object> item, Long itemId);

	void fixWFStatus(_Database _db, List<Map<String, Object>> items);

	SQLExcutor queryMyTasks(_Database _db, Map<String, String> params, int page, int pagesize);

	List<String> getUsers(List<Map<String, String>> rule, boolean justOne);

	void calculateWFStatus(Long itemId, _Database _db);
	
	void approve(Long recordId, Long id, String opinion);
	
	void reject(Long recordId, Long id, String opinion);
	
	void reApply(Long recordId, Long id, String opinion);
	
	void transApprove(Long recordId, Long id, String opinion, String target);
	
	void cancel(Long recordId, Long id, String opinion);

	List<?> getTransList(Long recordId, Long id);

}
