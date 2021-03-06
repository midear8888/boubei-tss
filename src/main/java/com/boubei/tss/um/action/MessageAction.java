/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.action;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.framework.persistence.pagequery.PageInfo;
import com.boubei.tss.framework.sms.MsgSender;
import com.boubei.tss.framework.web.display.grid.GridDataEncoder;
import com.boubei.tss.framework.web.mvc.BaseActionSupport;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.entity.Message;
import com.boubei.tss.um.helper.MessageQueryCondition;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.um.service.IMessageService;
import com.boubei.tss.util.XMLDocUtil;

@Controller
@RequestMapping("/auth/message")
public class MessageAction extends BaseActionSupport {
	
	protected Logger log = Logger.getLogger(this.getClass());
	
	@Autowired ILoginService loginService;
	@Autowired IMessageService messageService;
	
    @RequestMapping(value = "/email", method = RequestMethod.POST)
    @ResponseBody
    public void sendEmail(String title, String content, String receivers) {
    	MsgSender.sendMail(title, content, receivers, false);
    }
    
    @RequestMapping(value = "/email2", method = RequestMethod.POST)
    @ResponseBody
    public void sendHtmlEmail(String title, String content, String receivers) {
    	MsgSender.sendMail(title, content, receivers, true);
    }
    
    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public void sendMessage(String title, String content, String receivers, String category, String level) {
		messageService.sendMessage(title, content, receivers, category, level);
    }

    @RequestMapping(value = "/list/{page}")
    public void listMessages(HttpServletResponse response, MessageQueryCondition condition, @PathVariable int page) {
        condition.getPage().setPageNum(page);
        PageInfo pi = messageService.getBoxList(condition);
        
        Document template = XMLDocUtil.createDoc(UMConstants.MESSAGE_GRID);
		GridDataEncoder gridEncoder = new GridDataEncoder(pi.getItems(), template);
        print( new String[]{"MsgList", "PageInfo"}, new Object[]{gridEncoder, pi} );
    }
    
    @RequestMapping(value = "/list/json/{page}", method = RequestMethod.POST)
    @ResponseBody
    public List<?> listMessages2Json(MessageQueryCondition condition, @PathVariable int page) {
        condition.getPage().setPageNum(page);
        PageInfo pi = messageService.getBoxList(condition);
        
        return pi.getItems();
    }
    
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ResponseBody
    public Message getMessage(@PathVariable("id") Long id) {
    	return messageService.viewMessage(id);
    }
    
    @RequestMapping(value = "/batch/{ids}", method = RequestMethod.POST)
    @ResponseBody
    public String batchRead(@PathVariable("ids") String ids) {
    	messageService.batchRead(ids);
    	return "success";
    }
    
    @RequestMapping(value = "/num", method = RequestMethod.GET)
    @ResponseBody
    public int getUnReadMsgNum() {
    	return messageService.getUnReadMsgNum();
    }
    
    @RequestMapping(value = "/{ids}", method = RequestMethod.DELETE)
    @ResponseBody
    public String deleteMessage(@PathVariable("ids") String ids) {
    	messageService.deleteMessage(ids);
    	return "success";
    }
}
