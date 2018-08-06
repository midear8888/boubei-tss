package com.boubei.tss.dm.record.workflow;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.boubei.tss.dm.ddl._Database;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.framework.Global;
import com.boubei.tss.modules.timer.AbstractJob;
import com.boubei.tss.util.EasyUtils;

/**
 * 1、批量计算wfStatus，适合数据清洗进来的流程数据（逐个取出计算一遍即可）;
 *    报销 + 费用 审批后自动流到 付款流程 ---- 用ETL;
 *    
 *  com.boubei.tss.dm.record.workflow.WFJob | 0 07 * * * ? | 12,13
 */
public class WFJob extends AbstractJob {
	
	WFService wfService = (WFService) Global.getBean("WFService");
	RecordService recordService = (RecordService) Global.getBean("RecordService");

	// jobConfig 为 tableIds
	protected String excuteJob(String jobConfig, Long jobID) {
		
    	String[] jobConfigs = EasyUtils.split(jobConfig, ","); 
    	for(String _tableId : jobConfigs) {
    		Long tableID = EasyUtils.obj2Long(_tableId);
    		_Database _db = recordService.getDB(tableID);
    		
    		Map<String, String> params = new HashMap<String, String>();
    		params.put("sortField", "updatetime");
    		params.put("sortType", "onlynull");
    		
    		List<Map<String, Object>> result = _db.select(0, 0, params ).result;
    		for( Map<String, Object> row : result ) {
    			Long id = (Long) row.get("id");
    			wfService.calculateWFStatus(id, _db);
    		}
    		
    		String updateSQL = "update " + _db.table + " set updatetime = ? where updatetime is null";
			Map<Integer, Object> paramsMap = new HashMap<Integer, Object>();
			paramsMap.put(1, new Date());
			SQLExcutor.excute(updateSQL, paramsMap, _db.datasource);
		}
    	
    	return "success";
	}

}
