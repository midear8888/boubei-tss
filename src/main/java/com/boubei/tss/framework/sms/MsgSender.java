package com.boubei.tss.framework.sms;

import java.util.HashMap;
import java.util.Map;

import com.boubei.tss.cache.JCache;
import com.boubei.tss.cache.extension.threadpool.IThreadPool;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.MailUtil;

/**
 * 消息发送工具：异步发送，且和业务代码解耦合; 
 * 统一发到站内信及邮件（有注册邮箱的话），再由定时JOB通过短信、微信通知等发送出去；发完状态变为已读。
 */
public class MsgSender {
	
	static IThreadPool threadPool = JCache.getInstance().getThreadPool();
	
	public static void send(Map<String, Object> params, String receivers, String ...channels) {

		for(String channel : channels) {
			SendChannel task = (SendChannel) BeanUtil.newInstanceByName( channel );
			
			task.params = params;
			task.receivers = receivers;
			task.channel = channel;
			
			threadPool.excute(task);
		}
	}

	public static void sendMail(String title, String content, String receivers, boolean isHtml) {
		
		String[] info = MailUtil.parseReceivers(receivers);
		receivers = info[1];
		String mailServer = info[0];
		
		Map<String, Object> params = new HashMap<>();
		params.put("title", title);
		params.put("content", content);
		if( isHtml ) {
			params.put("isHtml", "true");
		}
		params.put("mailServer", mailServer);
		
		send(params, receivers, SendByMail.class.getName());
	}

}
