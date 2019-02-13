/* ==================================================================   
 * Created [2016-06-22] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.timer;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.boubei.tss.EX;
import com.boubei.tss.framework.sso.IOperator;
import com.boubei.tss.framework.sso.IdentityCard;
import com.boubei.tss.framework.sso.TokenUtil;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.modules.log.BusinessLogger;
import com.boubei.tss.modules.log.Log;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.helper.dto.OperatorDTO;
import com.boubei.tss.util.EasyUtils;

public abstract class AbstractJob implements Job {
	
	public static final String TIMER = "Timer";

	protected Logger log = Logger.getLogger(this.getClass());
	
	public boolean auto = false;
	
	/**
	 * 任务执行人
	 */
	protected IOperator jobRobot() {
        return new OperatorDTO(UMConstants.ROBOT_USER_ID, UMConstants.ROBOT_USER_NAME); 
	}
	
	protected boolean needSuccessLog() {
		return false;
	}
	
	protected void initContext() {
		// 模拟登录，用以初始化Environment
		IOperator excutor = jobRobot();
		String token = TokenUtil.createToken("1234567890", excutor.getId());
		IdentityCard card = new IdentityCard(token, excutor);
		Context.initIdentityInfo(card);
	}
	
    public void execute(JobExecutionContext context) throws JobExecutionException {
    	initContext(); 
		auto = true; 
    	
    	JobDetail aJob = context.getJobDetail();
    	String jobName = aJob.getKey().getName();
    	
    	JobDataMap dataMap = aJob.getJobDataMap();
    	String jobConfig = (String) dataMap.get(jobName);
    	Long jobID = (Long) dataMap.get(jobName + "-ID");
        
        log.info("Job[" + jobName + "] starting...");
        
        excuting(jobName, jobConfig, jobID);
    }

    // 执行Job并记录日志
	protected String excuting(String jobName, String jobConfig, Long jobID) {
		String resultMsg;
        Log excuteLog = null;
        
        try {
        	Long preTime = System.currentTimeMillis();
        	
        	resultMsg = excuteJob(jobConfig, jobID);
        	
        	int methodExcuteTime = (int) (System.currentTimeMillis() - preTime);
        	
        	log.info(resultMsg);
        	
        	String jcf = EasyUtils.obj2String(jobConfig);
			if( (needSuccessLog() && jcf.indexOf("noLog") < 0) || jcf.indexOf("needLog") >= 0) {
        		excuteLog = new Log(jobName + " - success", resultMsg);
            	excuteLog.setMethodExcuteTime(methodExcuteTime);
        	}
        } 
        catch(Exception e) {
        	resultMsg = "Job[" +jobName+ "] error: " + e.getMessage();
        	log.error(resultMsg, e);
        	
        	excuteLog = new Log(jobName + " - " + EX._ERROR_TAG, resultMsg);
        } 
        finally {
        	try {
        		if(excuteLog != null) {
        			excuteLog.setOperateTable(TIMER);
        			BusinessLogger.log(excuteLog); // 跑Test时可能没有spring IOC
        		}
        	} 
        	catch(Exception e) { }
        }
        
        return resultMsg;
	}

    protected abstract String excuteJob(String jobConfig, Long jobID);
    
    public void excuteJob(String jobConfig) {
    	excuteJob(jobConfig, null);
    }
}
