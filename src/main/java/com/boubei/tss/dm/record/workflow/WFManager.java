package com.boubei.tss.dm.record.workflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Element;

import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.ddl._Database;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.XMLDocUtil;

public class WFManager {
	
	public static WFStep getCurrStep(_Database _db, Map<String, Object> data) {
		// 加入特定用户、角色等信息
		data.put( "all", data.get("creator") ); // TODO 此流程数据所有参与人员，从WFLog里获取
		
		List<String> roles = Environment.getOwnRoleNames();
		for( String role : roles ) {
			data.put( role, role );
		}
		
		String _wf = DMUtil.freemarkerParse( _db.wfDefine.content, data );
		
		Document doc = XMLDocUtil.dataXml2Doc(_wf);
		List<Element> nodes = XMLDocUtil.selectNodes(doc, "/process/curstep");
		String currStep = nodes.get(0).getText();
		
		return _db.wfDefine.steps.get(currStep);
	}
	
	public static String getCurrStatus(_Database _db, Map<String, Object> data) {
		return getCurrStep(_db, data).status;
	}
	
	
	/**
	 * TODO 获取用户的同组某角色用户，比如此部门的部门经理
	 */
	public Object getSameGroupUserByRole() {
		return null;
	}
	
	public List<?> queryTasks4User(RecordService recordService, String userCode, Object record, 
			Map<String, String> params, int page, int pagesize) {
		
		Long recordId = recordService.getRecordID(record);
    	_Database _db = recordService.getDB(recordId);
    	List<Map<String, Object>> items = _db.select(page, pagesize, params).result;
    	List<Object> itemIds = new ArrayList<Object>();
    	Map<Long, Map<String, Object>> m = new HashMap<Long, Map<String, Object>>();
    	for( Map<String, Object> item : items ) {
    		Long id = EasyUtils.obj2Long(item.get("id"));
			itemIds.add(id);
    		m.put(id, item);
    	}
    	
    	String userName = Environment.getUserName();
		String sql = "SELECT * FROM wf_log WHERE " +
				" id IN (SELECT max(id) FROM wf_log WHERE tableId = ? and itemId in (" +EasyUtils.list2Str(itemIds)+ ") GROUP BY itemId ) " +
				" and nextStepProcesser like ? ";
		
		Map<Integer, Object> paramsMap = new HashMap<Integer, Object>();
    	paramsMap.put(1, recordId);
		paramsMap.put(2, "%" +userName+ "%");
    	List<Map<String, Object>> logs = SQLExcutor.query(DMConstants.LOCAL_CONN_POOL, sql, paramsMap);
    	
    	List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
    	for(Map<String, Object> log : logs) {
    		String[] nextStepProcessers = (log.get("nextstepprocesser")+"").split(",");
    		if(Arrays.asList(nextStepProcessers).contains(userName)) {
    			Long itemId = EasyUtils.obj2Long( log.get("itemid") );
    			result.add( m.get(itemId) );
    		}
    	}
    	
    	return result;
	}

}
