package com.boubei.tssx.sms;

import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.QuerySendDetailsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.QuerySendDetailsResponse;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.boubei.tss.cache.Cacheable;
import com.boubei.tss.cache.Pool;
import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.log.IBusinessLogger;
import com.boubei.tss.modules.log.Log;
import com.boubei.tss.modules.param.ParamConfig;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MathUtil;

public class AliyunSMS {
	
	private Logger log = Logger.getLogger(this.getClass());
 
    String sms_sign;
    IAcsClient acsClient;
    ICommonService commService;
    
    private static Pool cache = CacheHelper.getLongCache();
    
    public static AliyunSMS instance() {
    	String domain = Environment.getDomain();
    	String key = EasyUtils.checkNull(domain, "boubei") + "_sms";
    	
    	Cacheable cacheItem = cache.getObject(key);
		if(cacheItem == null) {
			cacheItem = cache.putObject(key, new AliyunSMS());
		}
    	
    	return (AliyunSMS) cacheItem.getValue();
    }
    
    private String getConfig(String code, String defaultVal) {
    	String val 	= (String) Environment.getDomainInfo("domain_" + code); 
        val = (String) EasyUtils.checkNull(val, ParamConfig.getAttribute(code, defaultVal) ); 
        return val;
    }
    
    private AliyunSMS() {
    	// 可自助调整超时时间
        System.setProperty("sun.net.client.defaultConnectTimeout", "10000");
        System.setProperty("sun.net.client.defaultReadTimeout", "10000");
        
        // 此处需要替换成开发者自己的AK(在阿里云访问控制台寻找)
        String sms_key 	  = getConfig("sms_key", null);
        String sms_secret = getConfig("sms_secret", null);
        sms_sign          = getConfig("sms_sign", null);
        
        // 初始化acsClient,暂不支持region化
        IClientProfile profile = DefaultProfile.getProfile("cn-hangzhou", sms_key, sms_secret);
        try {
        	// 产品名称:云通信短信API产品
			DefaultProfile.addEndpoint("cn-hangzhou", "cn-hangzhou", "Dysmsapi", "dysmsapi.aliyuncs.com");
		} catch (ClientException e) {
			log.error("create AliyunSMS error", e);
		}
        this.acsClient = new DefaultAcsClient(profile);
        
        commService = Global.getCommonService();
    }
    
    /** 
     * 大陆手机号码11位数，匹配格式. 
     */
    public static boolean isChinaPhoneLegal(String phone) {  
        String regExp = "^[1][3,4,5,6,7,8,9][0-9]{9}$";  
        Pattern p = Pattern.compile(regExp);  
        Matcher m = p.matcher(phone);  
        return m.matches();  
    } 

    /**
     * 按照短信模板发送短信
     * 
     * @param phone
     * @param tlCode 模板编号
     * @param tlParam  {“code”:”1234”,”product”:”ytx”}
     * @param outId 外部流水ID
     * @return
     */
    public SendSmsResponse send(String phone, String tlCode, String tlParam, Object outId) {
    	if( phone == null || !isChinaPhoneLegal(phone) ) {
    		return null;
    	}
    	
    	// 检查和最近一条短信的时间间隔，如有100秒内已经发送过的验证码，则不再重新发送（防止使坏）
    	Date from = DateUtil.subDays(new Date(), 1.6/(24*60)); // 96秒
    	List<?> list = commService.getList(" from SMSLog where phonenum = ? and createTime > ? ", phone, from);
    	if( list.size() > 0 ) {
    		return null;
    	}
    	
    	SendSmsRequest request = new SendSmsRequest();
        request.setPhoneNumbers(phone); //必填:待发送手机号
        request.setSignName(sms_sign); //必填:短信签名-可在短信控制台中找到
        
        request.setTemplateCode( tlCode );   // 必填：短信模板-可在短信控制台中找到，可定义多个模板
        request.setTemplateParam( tlParam ); // 可选：模板中的变量替换JSON串,如模板内容: "亲爱的${name},您的验证码为${code}"
        request.setOutId( outId + "");  // 可选：outId为提供给业务方扩展字段,最终在短信回执消息中将此值带回给调用者

        //hint 此处可能会抛出异常，注意catch
        SendSmsResponse ssr = null;
        try {
        	ssr = acsClient.getAcsResponse(request);
        	 
        	// 短信发送成功， 保存验证码、手机号、时间等信息到数据库
        	SMSLog log = new SMSLog();
        	log.setPhonenum(phone);
        	log.setRandomnum( EasyUtils.obj2Int(outId) );
        	log.setSendDay( DateUtil.format(new Date()) );
        	log.setMsg( ssr.getMessage() );
        	log.setTlcode(tlCode);
        	log.setParams(tlParam);
        	commService.create(log);
		} 
        catch (Exception e) {
        	logException(e);
		}
        
        return ssr;
    }
    
    // 不成功则记录异常
    public void logException(Exception e) {
    	String errMsg = "短信接口异常";
		log.error(errMsg, e);
    	
		Log excuteLog = new Log(e.getMessage(), e.getCause());
    	excuteLog.setOperateTable(errMsg);
        ((IBusinessLogger) Global.getBean("BusinessLogger")).output(excuteLog);
    }
    
    // 发送随机数验证码
    public SendSmsResponse sendRandomNum(String phone) {
    	int randomKey = MathUtil.randomInt(899999) + 100000;
    	String tlCode = getConfig("sms_verify", "SMS_133270155");
		String tlParam = "{\"code\":\"" +randomKey+ "\"}";
		
		SendSmsResponse ssr = send(phone, tlCode, tlParam, randomKey);
		ssr.setMessage( String.valueOf(randomKey) );
		return ssr;
    }
    
    public boolean checkCode(String mobile, Object code) {
    	Date from = DateUtil.subDays(new Date(), 10.0/(24*60));
    	List<?> list = commService.getList(" from SMSLog where phonenum = ? and randomnum = ? and createTime > ? ", 
    			mobile, EasyUtils.obj2Int(code), from);
    	
    	return list.size() > 0;
    }
    
    public QuerySendDetailsResponse querySendDetails(String phone, String bizId) throws ClientException {
        QuerySendDetailsRequest request = new QuerySendDetailsRequest();
        
        request.setPhoneNumber(phone); // 必填-号码
        request.setBizId(bizId);      // 可选-流水号
        
        request.setSendDate( DateUtil.format(new Date(), "yyyyMMdd")); // 必填-发送日期 支持30天内记录查询，格式yyyyMMdd
        request.setPageSize(10L); 
        request.setCurrentPage(1L);

        //hint 此处可能会抛出异常，注意catch
        return acsClient.getAcsResponse(request);
    }

}
