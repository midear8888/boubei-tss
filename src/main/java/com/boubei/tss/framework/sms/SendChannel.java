package com.boubei.tss.framework.sms;

import java.util.Map;
import java.util.regex.Pattern;

import com.boubei.tss.cache.extension.workqueue.AbstractTask;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.um.entity.Message;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.util.EasyUtils;

public class SendChannel extends AbstractTask {
	
	protected ILoginService loginService;
	protected ICommonService commService;
	
	public Map<String, Object> params;
	public String receivers;
	public String channel;

	protected String[] receiverIds;

	Pattern compile = Pattern.compile("^[\\d\\,-]*$");

	public void excute() {
		
		commService = Global.getCommonService();
		loginService = (ILoginService) Global.getBean("LoginService");
		receiverIds = loginService.getContactInfos(receivers, true);

		if( receiverIds.length == 0 && compile.matcher(receivers).matches() ) {
			receiverIds = receivers.split(","); // 本来就是ID
		}
		
		send();
	}

	protected void send() {
		
		Message msg = new Message();
		msg.setSendChannel( channel );
		msg.setTitle((String) params.get("title"));
		msg.setContent((String) params.get("content"));
		
		for( String receiverId : receiverIds ) {
			Long _receiverId = EasyUtils.obj2Long(receiverId);
			msg.setReceiverId( _receiverId );
			msg.setReceiver( loginService.getOperatorDTOByID(_receiverId).getUserName() );
			
			msg.setId( null );
			commService.create(msg);
		}
		
	}
}
