package com.boubei.tss.framework.sms;

import java.util.Date;

import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MathUtil;

public class DefaultSMS extends AbstractSMS {

	public Object send(String phone, String tlCode, String tlParam, Object outId) {
		// 短信发送成功， 保存验证码、手机号、时间等信息到数据库
    	SMSLog log = new SMSLog();
    	log.setPhonenum(phone);
    	log.setRandomnum( EasyUtils.obj2Int(outId) );
    	log.setSendDay( DateUtil.format(new Date()) );
    	log.setTlcode(tlCode);
    	log.setParams(tlParam);
    	commService.create(log);
    	
    	return null;
	}

	public Object sendRandomNum(String phone) {
		int randomKey = MathUtil.randomInt(899999) + 100000;
		send(phone, null, null, randomKey);
		
		return randomKey;
	}

}
