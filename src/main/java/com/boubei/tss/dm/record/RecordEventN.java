package com.boubei.tss.dm.record;

import java.util.Map;

import com.boubei.tss.EX;
import com.boubei.tss.dm.ddl._Database;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.log.BusinessLogger;
import com.boubei.tss.util.EasyUtils;

public class RecordEventN implements RecordEvent {
	
	public void beforeSelect(Map<String, String> params, _Database db) {
		// do nothing
	}
	
	public void afterSelect(SQLExcutor ex, _Database db) {
		// do nothing
	}

	public void beforeInsert(Map<String, String> valuesMap) {
		// do nothing
	}

	public void afterInsert(Long id, Map<String, String> item, _Database db) {
		log( EasyUtils.obj2String(id), "create", " add a new row: " + item, db);
	}

	public  Map<String, Object> beforeUpdate(Long id, Map<String, String> valuesMap, _Database db) {
		Map<String, Object> old = db.get(id);
		if( old == null ) {
			throw new BusinessException(EX.DM_16);
		}
		return old;
	}

	public void afterUpdate(Long id, Map<String, Object> item, _Database db) {
		log(id, "update", "\n begin: " + item + " \n after: " + db.get(id), db);
	}

	public  Map<String, Object> beforeDelete(Long id, _Database db) {
		return db.get(id);
	}

	public void afterDelete(Long id, Map<String, Object> item, _Database db) {
		log(id, "delete", Environment.getUserCode() + " deleted one row：" + item, db);
	}
	
	public void afterLogicDelete(Long id, Map<String, Object> item, _Database db) {
		log(id, "logicDelete", Environment.getUserCode() + " deleted one row：" + item, db);
	}
	
	public static void log(Object id, String opeartion, String logMsg, _Database db) {
		if( db.needLog ) {
			BusinessLogger.log(db.recordName, opeartion + ", " + id, logMsg);
		}
	}
}
