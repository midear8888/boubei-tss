/* ==================================================================   
 * Created [2018-10-16] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.record.workflow;

import java.util.Map;

import com.boubei.tss.dm.ddl._Database;

/**
 * 流程自定义监听事件
 * 
 * wfEventClass:=com.boubei.xx.xx
 */
public interface WFEvent {
	
	void after( Map<String, Object> item, WFStatus wfStatus, _Database db );

}
