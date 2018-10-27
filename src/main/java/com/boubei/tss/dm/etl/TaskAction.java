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

import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.timer.JobService;

@Controller
@RequestMapping("/auth/task")
public class TaskAction {
	
	@Autowired ICommonService commonService;
	@Autowired JobService jobService;
	
	@RequestMapping(value = "/{idOrName}", method = RequestMethod.POST)
	@ResponseBody
	public Object exucteTask(@PathVariable String idOrName) {
		return jobService.excuteTask( idOrName, Environment.getUserId() );
	}
	
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	@ResponseBody
	public void disableTask(@PathVariable Long id) {
		Task task = (Task) commonService.getEntity(Task.class, id);
		task.setStatus(Task.STATUS_OFF);
		commonService.updateWithLog(task);
	}
	
	@RequestMapping(value = "/{id}", method = RequestMethod.PUT)
	@ResponseBody
	public void enableTask(@PathVariable Long id) {
		Task task = (Task) commonService.getEntity(Task.class, id);
		task.setStatus(Task.STATUS_ON);
		commonService.updateWithLog(task);
	}
}
