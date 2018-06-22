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

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.EX;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.Global;
import com.boubei.tss.modules.param.ParamListener;
import com.boubei.tss.modules.param.ParamManager;
import com.boubei.tss.util.BeanUtil;

/**
 
var m2 = { label:"立即执行",  callback: runJob, icon: ICON + "icon_start.gif"};
var m3 = { label:"停用", callback: function() { setJobStatus("1"); }, visible:function() { return $.G("grid").getColumnValue("disabled") == '0'; }, icon: "images/icon_stop.gif"  };
var m4 = { label:"启用", callback: function() { setJobStatus("0"); }, visible:function() { return $.G("grid").getColumnValue("disabled") == '1'; }, icon: "images/icon_start.gif"  };
$1("grid").contextmenu.addItem(m2);
$1("grid").contextmenu.addItem(m3);
$1("grid").contextmenu.addItem(m4);

var queue = {};
function runJob() {
     var id = $.G("grid").getColumnValue("id");
     !queue[id] && $.post('/tss/auth/job/' + id, {}, function(data) {  delete queue[id]; $.alert(data); } );
     queue[id] = "running";
}
function setJobStatus(status) {
     $.ajax({
         url: '/tss/xdata/component_job_def/' + $.G("grid").getColumnValue("id"), 
         params: {"disabled": status}, 
         onsuccess: function() {  $.alert("操作成功"); loadGridData(1); } 
      });
}
 */
@Controller
@RequestMapping("/auth/job")
public class JobAction {
	
	@RequestMapping(value = "/{key}", method = RequestMethod.POST)
	@ResponseBody
	public Object exucteJob(@PathVariable String key) {
		
		List<?> list = Global.getCommonService().getList("from JobDef where ? in (id, code) and disabled <> 1 ", key);
		if( list.isEmpty() ) {
			return EX.EXCEPTION;
		}
		
		JobDef jobDef = (JobDef) list.get(0);
		String jobClass = jobDef.getJobClassName();
		
		Object job = BeanUtil.newInstanceByName(jobClass);
		((AbstractJob)job).excuteJob( jobDef.getCustomizeInfo(), jobDef.getId() );
		
		return EX.DEFAULT_SUCCESS_MSG;
	}
	
	@RequestMapping(value = "/refresh", method = RequestMethod.POST)
	@ResponseBody
	public Object refresh() {
		for( ParamListener bean : ParamManager.listeners) {
			if(bean instanceof SchedulerBean) {
				((SchedulerBean)bean).refresh(false);
			}
		}
		return "Success";
	}
	
	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public List<Map<String, Object>> listJobs() {
		String sql = "select id, id as value, name from component_job_def " +
				" where disabled=0 and jobClassName like '%etl%' order by name";
		return SQLExcutor.queryL(sql);
	}
}
