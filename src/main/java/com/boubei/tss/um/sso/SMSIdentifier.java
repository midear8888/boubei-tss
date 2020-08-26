/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.sso;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.boubei.tss.EX;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sms.AbstractSMS;
import com.boubei.tss.framework.sso.IOperator;
import com.boubei.tss.framework.sso.PasswordPassport;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.framework.sso.identifier.BaseUserIdentifier;
import com.boubei.tss.framework.web.display.xmlhttp.XmlHttpEncoder;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.helper.dto.OperatorDTO;
import com.boubei.tss.um.service.ILoginService;

/**
 * 短信验证码身份认证器: com.boubei.tss.um.sso.SMSIdentifier
 * 
 */
public class SMSIdentifier extends BaseUserIdentifier {
    
    protected Logger log = Logger.getLogger(this.getClass());
    
    protected ILoginService loginservice = (ILoginService) Global.getBean("LoginService");
    
    public Object before(User user, XmlHttpEncoder encoder, HttpSession session) {
    	int randomKey = 100;
        encoder.put(SSOConstants.RANDOM_KEY, randomKey);
        session.setAttribute(SSOConstants.RANDOM_KEY, randomKey);
    	
        return AbstractSMS.create().sendRandomNum( user._mobile() );
	}
    
    protected IOperator validate() throws BusinessException {
    	PasswordPassport passport = new PasswordPassport();
        String loginName = passport.getLoginName(); // loginName/email/mobile
        String passwd = passport.getPassword();
        
        User user = loginservice.getLoginInfoByLoginName(loginName);
		boolean result = AbstractSMS.create().checkCode(user._mobile(), passwd);
        
        // 验证都不通过
		if ( !result ) {
			throw new BusinessException( EX.U_51 );
        }
		
		return new OperatorDTO(user);
    }
}
