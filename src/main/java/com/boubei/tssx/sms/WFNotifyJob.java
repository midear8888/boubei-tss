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
		
		String sql = "select u.telephone, u.userName, count(*) num from dm_workflow_status t, um_user u" +
				" where t.nextProcessor = u.loginName and u.telephone is not null and applyTime >= DATE_SUB(NOW(), INTERVAL 48 hour) " +
				" group by nextProcessor ";
		
		List<Map<String, Object>> list = SQLExcutor.queryL(sql);
		for(Map<String, Object> row : list ) {
			String phone = (String) row.remove("telephone");
			row.put("sysName", ParamManager.getValue("sysTitle", "它山石"));
			row.put("userName", row.remove("username"));
			
			String tlParam = EasyUtils.obj2Json(row);
			AliyunSMS.instance().send(phone, tlCode, tlParam, -1);
		}
		
		return "共发送" + list.size() + "条流程短信提醒。";
	}

}
