package com.boubei.tssx;

import java.util.List;
import java.util.Map;

import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.modules.timer.AbstractJob;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MailUtil;

/**
 * 监控：数据库（MySQL/Oracle等）连接及主从同步、ETL数据抽取、Apache、Tomcat、WebService等
 * 
 * 生成异常信息放到系统异常日志里，再通过定时任务发送出去。
 * 每30分钟，轮询最近30分钟 Monitor-Err 日志， 有的话发邮件出来。
 * 
 * 配置步骤（可监视多台服务器）：
 * 1、Job：com.boubei.tssx.MonitorJob | 0 0/30 * * * ? | 10,www.boubei.com,卜贝
 * 10,tms.boudata.com,车队管理
 * 2、系统参数： Monitoring-Receivers
 */
public class MonitorJob extends AbstractJob {

	protected String excuteJob(String jobConfig, Long jobID) {
		String[] jobConfigs = EasyUtils.split(jobConfig, "\n");
		
		for(String jobX : jobConfigs) {
			String[] info = jobX.split(",");
			if(info.length != 3) continue;
			
			int interval   = Integer.parseInt(info[0]);  // 间隔时间（分钟）
			String domain  = info[1];
			String sysName = info[2];
			
			monitoringMySQL();
			monitoringApache(domain);
			monitoringTomcat(domain, sysName);
			
			checking(DMConstants.LOCAL_CONN_POOL, sysName, "Monitor-Err", "", interval);
			checking(DMConstants.LOCAL_CONN_POOL, sysName, "ETL-Err", "", interval);
			checking(DMConstants.LOCAL_CONN_POOL, sysName, "定时任务", "and t.operationCode like '%【失败!!!】%'", interval);
		}
		
		return "done";
	}
	
	/**
	 * 每30分钟，轮询最近30分钟 Monitor-Err 日志， 有的话发邮件出来
	 * checking(DMConstants.LOCAL_CONN_POOL, sysName, "定时任务", "and t.operationCode like '%【失败!!!】%'");
	 */
	public void checking(String ds, String sysName, String errName, String fitler, int interval) {
		String sql = "select operationCode 类型, content 内容, operateTime 监测时间 " +
				" from component_log t   " +
				" where t.operateTable = '" +errName+ "' " + fitler +
				"  and t.operateTime > DATE_SUB(NOW(), INTERVAL " +(interval+3)+ " MINUTE)";
		List<Map<String, Object>> errList = SQLExcutor.query(ds, sql);
		if(errList.isEmpty()) return;
		
		String content = "", title = "异常提醒：" + sysName + "，" +errName+ ": ";
		for(Map<String, Object> log : errList) {
			content += log + " \n ";
			title += log.get("类型") + "|";
		}
		
		MailUtil.send(title, content,  MonitorUtil.getReceiver() , "sys");
	}
	
	/** 主从同步、是否宕机 */
	void monitoringMySQL() {
//		MonitorUtil.monitoringMySQL("connpool-tssbi-master", "connpool-tssbi-slave");
		MonitorUtil.testDBConn( DMConstants.LOCAL_CONN_POOL );
		
		log.info("monitoring MySQL finished. ");
	}

	/** 文件夹同步（交由 crontab 来监控） */
	void monitoringFileRsync() { }

	/** 
	 * Manage页Tomcat状态变成err等各种异常） 
	 */
	void monitoringApache(String domain) {
		if( !"www.boubei.com".equals(domain) ) {  // boubei.com 是 nginx
			MonitorUtil.monitoringApache( domain ); 
		}
		log.info("monitoring Apache finished. ");
	}
	
	/** 
	 * 访问 param/json/simple/sysTitle、si/version 服务，返回object数组，以检查 Tomcat是否正常 
	 */
	void monitoringTomcat(String domain, String sysName) {
		MonitorUtil.monitoringRestfulUrl("http://" +domain+ "/tss/si/version", null);
		MonitorUtil.monitoringRestfulUrl("http://" +domain+ "/tss/param/json/simple/sysTitle", sysName);
		
		log.info("monitoring Tomcat finished. ");
	}
}
