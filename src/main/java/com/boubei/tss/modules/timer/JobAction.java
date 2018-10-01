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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.Global;
import com.boubei.tss.util.EasyUtils;

/**
 
var m2 = { label:"立即执行",  callback: runJob, icon: ICON + "icon_start.gif"};
var m3 = { label:"停用", callback: function() { setJobStatus("1"); }, visible:function() { return $.G("grid").getColumnValue("disabled") == '0'; }, icon: "images/icon_stop.gif"  };
var m4 = { label:"启用", callback: function() { setJobStatus("0"); }, visible:function() { return $.G("grid").getColumnValue("disabled") == '1'; }, icon: "images/icon_start.gif"  };
$1("grid").contextmenu.addItem(m2);
$1("grid").contextmenu.addItem(m3);
$1("grid").contextmenu.addItem(m4);

function runJob() {
     var id = $.G("grid").getColumnValue("id");
     $.post('/tss/auth/job/' + id, {}, function(data) {  $.alert(data); } );
}
function setJobStatus(status) {
     $.ajax({
         url: '/tss/xdata/component_job_def/' + $.G("grid").getColumnValue("id"), 
         params: {"disabled": status}, 
         onsuccess: function() {  
             $.alert("操作成功"); 
             loadGridData(1); 
             $.post("/tss/auth/job/refresh", {}, function(msg) { msg == 'Success' && $.tssTip( "定时器刷新成功" );  });
         } 
      });
}
 */
@Controller
@RequestMapping("/auth/job")
public class JobAction {
	
	@Autowired JobService jobService;
	
	@RequestMapping(value = "/{key}", method = RequestMethod.POST)
	@ResponseBody
	public Object exucteJob(@PathVariable String key) {
		Object tag = 0;
		try {
			Long.parseLong(key);  // 按Job id查询，通常是在Job列表页面里，没有业务关联性；防止反复点击
		} catch(Exception e) {
			tag = System.currentTimeMillis();  // 按Job Code刷新通常是跟着业务逻辑（比如人员组织角色变动了需要同步，需要实时）
		}
		
		return jobService.excuteJob(key, tag); 
	}
	
	@RequestMapping(value = "/refresh", method = RequestMethod.POST)
	@ResponseBody
	public Object refresh() {
		Global.schedulerBean.refresh(false);
		
		return "Success";
	}
	
	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public List<Map<String, Object>> listJobs(String tag) {
		tag = EasyUtils.obj2String(tag);
		
		String sql = "select id, id as value, name from component_job_def " +
				" where disabled = 0 and jobClassName like '%etl%' and jobClassName like '%" +tag+ "%' order by name";
		return SQLExcutor.queryL(sql);
	}
}
