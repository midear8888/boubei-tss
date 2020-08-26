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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import com.boubei.tss.PX;
import com.boubei.tss.framework.Config;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.util.BeanUtil;

/** 
 * 定时器调度。
 * 
 * 新增或删除一个job失败,不影响其它job的生成和删除。
 * 
 */
@SuppressWarnings("unchecked")
public class SchedulerBean {
    
	protected Logger log = Logger.getLogger(this.getClass());
	
    private Scheduler scheduler;
    private Map<String, JobDef> defsMap;
    
    private static SchedulerBean sb;
    public  static SchedulerBean getInstanse() {
    	if( sb == null) {
    		sb = new SchedulerBean();
    	}
    	return sb;
    }
    
    SchedulerBean() {
    	// 根据配置决定是否启用定时Job
    	if( Config.TRUE.equals(Config.getAttribute(PX.ENABLE_JOB)) ) {

    		log.info("SchedulerBean is starting....." );
	    	
	    	defsMap = new HashMap<String, JobDef>();
	    	try {
				scheduler = StdSchedulerFactory.getDefaultScheduler();
				scheduler.start();
			} catch (SchedulerException e) {
	            throw new BusinessException("start SchedulerBean error", e);
	        } 
	    	
	    	refresh(true);
    	}
    }
    
    public List<JobExecutionContext> listExcutingJobs() {
    	List<JobExecutionContext> list = null;
		try {
			list = scheduler.getCurrentlyExecutingJobs();
		} catch (Exception e) {
		}
		return list;
    }
    
    public void refresh(boolean init) {
    	if(scheduler == null) return; // job_enable = false
    	
    	log.debug("SchedulerBean refresh begin...");
    	
		List<JobDef> list = (List<JobDef>) Global.getCommonService().getList(" from JobDef ");
    	
        List<String> jobCodes = new ArrayList<String>();
		for(JobDef def : list) {
			
			String code  = def.getCode();
			String jobName = code;
			jobCodes.add( code );
			
			// 如果已经生成且没做过修过（包括没被停用），则不变
			if( def.equals(defsMap.get(code)) ) { 
				continue; 
			} 
			// 如果已经存在，且value发生了变化，则先删除旧的Job，重新生成新的Job
			else if(defsMap.containsKey(code)) {
				deleteJob(code); 
			}
			
			if( def.isDisabled() ) {
				continue; // 停用的JOB剔除后无需再生成
			}
			
			// 新增或修过过的定时配置，需要重新生成定时Job
			try {
				Class<Job> jobClazz = (Class<Job>) BeanUtil.createClassByName(def.getJobClassName());
				JobDetail aJob = JobBuilder.newJob(jobClazz)
						.withIdentity(jobName)
						.usingJobData(jobName, def.getCustomizeInfo())
						.usingJobData(jobName + "-ID", def.getId())
						.build();
				
				String ts = def.getTimeStrategy(); // 定时策略
				Trigger trigger = TriggerBuilder.newTrigger().withSchedule( CronScheduleBuilder.cronSchedule(ts) ).build(); 
				scheduler.scheduleJob(aJob, trigger);
				
				log.info(" scheduler.scheduleJob: " + jobName + " successed. timeStrategy=" + ts );
				
				JobDef copy = new JobDef();
				BeanUtil.copy(copy, def);
				defsMap.put(code, copy);
			} 
			catch (Exception e) {
				log.error("init Job[" + jobName + "] failed, config = " + def, e);
			}  
		}
		
		Set<String> deleteJobCodes = new HashSet<String>(defsMap.keySet());
		deleteJobCodes.removeAll(jobCodes);
		for(String code : deleteJobCodes) {
			deleteJob(code); // 停用/删除的定时配置
		}
        
        log.debug("SchedulerBean init end.");
    }
    
    private void deleteJob(String jobName) {
    	try {
			JobKey key = new JobKey(jobName);
			scheduler.deleteJob(key);
			
			defsMap.remove(jobName);
			log.info(" scheduler.deleteJob: " + jobName + " successed." );
		} 
		catch (SchedulerException e) {
			log.error("remove Job[" + jobName + "] failed", e);
		}
    }
}

