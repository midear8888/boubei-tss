/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.service;

import java.util.List;

import com.boubei.tss.framework.persistence.pagequery.PageInfo;
import com.boubei.tss.um.entity.Message;
import com.boubei.tss.um.helper.MessageQueryCondition;
 
public interface IMessageService {
 
	/**
	 * 发送短消息
	 * @param message
	 */
	void sendMessage(String title, String content, String receivers);
	void sendMessage(String title, String content, String receivers, String category, String level);
	
	/**
	 * 查看短消息 并将标志位改成已读
	 * @param id
	 * @return
	 */
	Message viewMessage(Long id);
	
	/**
	 * 批量设置消息为已阅
	 * @param id
	 */
	void batchRead(String ids);
	
	/**
	 * 删除短消息
	 * @param id
	 */
	void deleteMessage(String id);
	
	int getUnReadMsgNum();
	
	List<?> getUnReadHignLevelMsg(int days);
	
	/**
	 * 获取收(发)件箱列表
	 */
	PageInfo getBoxList(MessageQueryCondition condition);
}