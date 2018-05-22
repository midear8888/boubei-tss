/* ==================================================================   
 * Created [2016-06-22] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.etl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.EX;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.modules.timer.JobDef;
import com.boubei.tss.util.BeanUtil;

@Controller
@RequestMapping("/auth/task")
public class TaskAction {
	
	@Autowired ICommonService commonService;
	
	@RequestMapping(value = "/{id}", method = RequestMethod.POST)
	@ResponseBody
	public Object exucteTask(@PathVariable Long id) {
		Task task = (Task) commonService.getEntity(Task.class, id);
		JobDef jobDef = (JobDef) commonService.getEntity(JobDef.class, task.getJobId());
		String jobClass = jobDef.getJobClassName();
		
		Object job = BeanUtil.newInstanceByName(jobClass);
		if( job instanceof AbstractETLJob ) {
			TaskLog log = ((AbstractETLJob)job).excuteTask(task);
			if( log == null ) {
				return EX.DM_30;
			}
			else if( "yes".equals( log.getException() ) ) {
				throw new BusinessException(task.getJobName() + EX._ERROR_TAG + log.getDetail() );
			}
		}
		else {
			throw new BusinessException( EX.parse(EX.DM_28, task.getJobId(), task.getJobName()) );
		}
		
		return EX.DEFAULT_SUCCESS_MSG;
	}
	
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	@ResponseBody
	public void disableTask(@PathVariable Long id) {
		Task task = (Task) commonService.getEntity(Task.class, id);
		task.setStatus(Task.STATUS_OFF);
		commonService.update(task);
	}
	
	@RequestMapping(value = "/{id}", method = RequestMethod.PUT)
	@ResponseBody
	public void enableTask(@PathVariable Long id) {
		Task task = (Task) commonService.getEntity(Task.class, id);
		task.setStatus(Task.STATUS_ON);
		commonService.update(task);
	}
}
