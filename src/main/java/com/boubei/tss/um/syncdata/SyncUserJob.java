/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.syncdata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.modules.progress.Progress;
import com.boubei.tss.modules.progress.Progressable;
import com.boubei.tss.modules.timer.AbstractJob;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.service.IGroupService;
import com.boubei.tss.util.EasyUtils;

/**
 * 自动同步用户
 * 
 * com.boubei.tss.um.syncdata.SyncUserJob | 0 06 * * * ? | 4
 * 
 * /tss-4.x/src/main/resources/template/um/syncdata/template_DB2.xml
 */
public class SyncUserJob extends AbstractJob {
	
	ISyncService syncService = (ISyncService) Global.getBean("SyncService");
	IGroupService groupService = (IGroupService) Global.getBean("GroupService");
 
	/* 
	 * jobConfig的格式为 : 
	 * 		mainGroupId1
	 * 		mainGroupId2
	 */
	protected String excuteJob(String jobConfig, Long jobID) {
		log.info("------------------- 用户信息自动同步......");
		
		String[] jobConfigs = EasyUtils.split(jobConfig, "\n");
		List<String> msgList = new ArrayList<String>();
		
		for(int i = 0; i < jobConfigs.length; i++) {
			if( EasyUtils.isNullOrEmpty(jobConfigs[i]) ) continue;
			 
			String info[] = EasyUtils.split(jobConfigs[i], ",");
			Long groupId = EasyUtils.obj2Long(info[0]);
			Group group = groupService.getGroupById(groupId);
			if ( group == null) {
	            throw new BusinessException( "用户同步配置异常，找不到组，jobConfig=" + jobConfig );
	        }
			
	        Map<String, Object> datasMap = syncService.getCompleteSyncGroupData(groupId);
	        String result = ((Progressable) syncService).execute(datasMap, new Progress(10000));
	        
	        msgList.add( "【" + jobConfig + "】" + result);
		}
		
		log.info("------------------- 用户信息自动同步 Done");
		return EasyUtils.list2Str(msgList);
	}
}
