package com.boubei.tssx.sms;

import java.util.List;
import java.util.Map;

import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.modules.param.ParamManager;
import com.boubei.tss.modules.timer.AbstractJob;
import com.boubei.tss.util.EasyUtils;

/**
 * com.boubei.tssx.sms.WFNotifyJob | 0 0 9 * * ? | X
 */
public class WFNotifyJob extends AbstractJob {

	protected String excuteJob(String jobConfig, Long jobID) {
		
		String tlCode = "SMS_138068571";
		String rejectCode = "SMS_147436558";
		
		String waitSql = "select u.telephone, u.userName, count(*) num from dm_workflow_status t, um_user u" +
				" where t.nextProcessor = u.loginName and u.telephone is not null and applyTime >= DATE_SUB(NOW(), INTERVAL 48 hour) " +
				" group by nextProcessor ";
		String rejectSql = "select  u.telephone, u.userName, count(*) num,group_concat( distinct tableName separator ',' ) tn from dm_workflow_status t, um_user u" +
				" where t.applier = u.loginName and u.telephone is not null and currentStatus = '已驳回' and lastProcessTime >= DATE_SUB(NOW(), INTERVAL 24 hour)" +
				" group by applier ";
		
		List<Map<String, Object>> waitList = SQLExcutor.queryL(waitSql);
		for(Map<String, Object> row : waitList ) {
			String phone = (String) row.remove("telephone");
			row.put("sysName", ParamManager.getValue("sysTitle", "它山石"));
			row.put("userName", row.remove("username"));
			
			String tlParam = EasyUtils.obj2Json(row);
			AliyunSMS.instance().send(phone, tlCode, tlParam, -1);
		}
		
		List<Map<String, Object>> rejectList = SQLExcutor.queryL(rejectSql);
		for(Map<String, Object> row : rejectList ) {
			String phone = (String) row.remove("telephone");
			row.put("sysName", ParamManager.getValue("sysTitle", "它山石"));
			row.put("userName", row.remove("username"));
			row.put("wflist", row.remove("tn"));
			
			String tlParam = EasyUtils.obj2Json(row);
			AliyunSMS.instance().send(phone, rejectCode, tlParam, -1);
		}
		
		return "共发送" + (waitList.size() + rejectList.size()) + "条流程短信提醒。";
	}

}
