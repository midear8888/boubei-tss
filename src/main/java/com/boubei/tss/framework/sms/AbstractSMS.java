package com.boubei.tss.framework.sms;

import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.modules.log.BusinessLogger;
import com.boubei.tss.modules.param.ParamConfig;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;

public abstract class AbstractSMS {
	
	private Logger log = Logger.getLogger(this.getClass());
 
    protected ICommonService commService;
    
    /** 异常信息转换器实现类类名属性名 */
    static final String SMS_CLASS_NAME = "class.name.SMS";
    
    static AbstractSMS instance = null;
    
    public static AbstractSMS create() {
        if (instance == null) {
            String className = ParamConfig.getAttribute(SMS_CLASS_NAME, DefaultSMS.class.getName());
            instance = (AbstractSMS) BeanUtil.newInstanceByName(className);
            instance.commService = Global.getCommonService();
        }
        return instance;
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
    public abstract Object send(String phone, String tlCode, String tlParam, Object outId);
    
    /**
     * 发送随机数验证码
     * 
     * @param phone
     * @return
     */
    public abstract Object sendRandomNum(String phone);
    
    /**
     * 验证短信验证码是否有效
     * 
     * @param mobile
     * @param code
     * @return
     */
    public boolean checkCode(String mobile, Object code) {
    	Date from = DateUtil.subDays(new Date(), 6.0/(24*60));
    	List<?> list = commService.getList(" from SMSLog where phonenum = ? and randomnum = ? and createTime > ? ", 
    			mobile, EasyUtils.obj2Int(code), from);
    	
    	return list.size() > 0;
    }
 
    // 发送不成功则记录异常
    protected void logException(Exception e) {
    	String errMsg = "短信接口异常";
		log.error(errMsg, e);
		BusinessLogger.log(errMsg, e.getMessage(), e.getCause());
    }
    
    /** 
     * 大陆手机号码11位数，匹配格式. 
     */
    public static boolean isChinaPhoneLegal(String phone) {  
    	if(EasyUtils.isNullOrEmpty(phone)) return false;
    	
        String regExp = "^[1][3,4,5,6,7,8,9][0-9]{9}$";  
        Pattern p = Pattern.compile(regExp);  
        Matcher m = p.matcher(phone);  
        return m.matches();  
    } 
}
