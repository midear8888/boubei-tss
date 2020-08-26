package com.boubei.tss.framework.sms;

import com.boubei.tss.util.MailUtil;

public class SendByMail extends SendChannel {

	protected void send() {
		
		super.send();
		
    	String[] emails = loginService.getContactInfos(receivers, false);
    	
    	if(emails != null && emails.length > 0) {
    		String title = (String) params.get("title");
			String content = (String) params.get("content");
			String mailServer = (String) params.get("mailServer");
			
    		if( params.containsKey("isHtml") ) {
    			MailUtil.sendHTML(title, content, emails, mailServer);
    		} else {
    			MailUtil.send(title, content, emails, mailServer);
    		}
    	}
	}

}
