package com.boubei.tssx.sms;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.boubei.tss.AbstractTest4;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;

public class SMSTest extends AbstractTest4 {
	
	static String phone = "1358883383*";
	
	@Test
	public void test() throws Exception {
		AliyunSMS sms = AliyunSMS.instance();
		sms = AliyunSMS.instance();
		
		String today = DateUtil.format(new Date());
		
		/* 短信验证码测试 */
		SendSmsResponse ssr = sms.sendRandomNum( phone );
		String code = ssr.getCode();
		List<?> list = super.commonDao.getEntities(" from SMSLog where randomnum = ? and sendDay = ? ", Integer.parseInt(code), today);
		
		Assert.assertEquals(1, list.size());
		log.info( EasyUtils.obj2Json(list) );
		
		// 100秒内部允许重复发
		ssr = sms.sendRandomNum( phone );
		Assert.assertNull(ssr);
		list = super.commonDao.getEntities(" from SMSLog where sendDay = ?", today );
		Assert.assertEquals(1, list.size());
		
		sms.querySendDetails(phone, code);
		
		// 检查
		Assert.assertTrue( sms.checkCode(phone, code) );
		
		// wrong phone num
		ssr = sms.sendRandomNum( "123456" ); 
		Assert.assertNull(ssr);
		new SMSLog().setId(null);
		
		sms.logException( new BusinessException("test") );
	}
	
	@Test
	public void test1() throws Exception {
		CallSMS caller = new CallSMS();
		
		request.addParameter("phone", phone);
		caller.doPost(request, response);
		
		request.addParameter("smsMsg", "xxx");
		request.addParameter("tlCode", "xxx");
		caller.doPost(request, response);
	}
	
//	@Test
	public void test2() {
		AliyunSMS sms = AliyunSMS.instance();
		sms = AliyunSMS.instance();

		/* 
		 * Dear customer, the goods sent to you by ${country} are shipped by CHH shipping, and the order number is ${code}. 
		 * You can adopt ${address} inquiry 尊敬的客户由${sender}发给您的货物由CHH航运揽收，订单号${order}，您可登陆查询
		 * 短信内容存在黑名单关键字
		 */
		Map<String, String> params = new HashMap<String, String>();
		params.put("country", " TAI GUO ");
		params.put("code", " YT1806230003 ");
		params.put("address", " cotels ");
		params.put("country_cn", " 柬埔寨 ");
		params.put("code_cn", " YT1806230003 cotels ");
		
		sms.send(phone, "SMS_138070788", EasyUtils.obj2Json(params), "001");
	}
}
