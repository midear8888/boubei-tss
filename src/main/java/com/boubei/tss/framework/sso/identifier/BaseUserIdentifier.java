/* ==================================================================   
 * Created [2009-7-7] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.sso.identifier;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.boubei.tss.EX;
import com.boubei.tss.framework.exception.UserIdentificationException;
import com.boubei.tss.framework.sso.IOperator;
import com.boubei.tss.framework.sso.IUserIdentifier;
import com.boubei.tss.framework.sso.IdentityCard;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.framework.sso.TokenUtil;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.framework.sso.online.OnlineUserManagerFactory;
import com.boubei.tss.framework.web.display.xmlhttp.XmlHttpEncoder;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.util.MathUtil;

/**
 * 身份认证器超类
 */
public abstract class BaseUserIdentifier implements IUserIdentifier {
    
    protected Logger log = Logger.getLogger(this.getClass());

	public IdentityCard identify() throws UserIdentificationException {
		// 验证通行证
		IOperator operator = validate();
		
		// 如果合法，注册在线用户，并获取相关用户信息对象，放入身份对象
		if (operator == null) {
			throw new UserIdentificationException(EX.U_49);
		}
		
		String appCode   = Context.getApplicationContext().getCurrentAppCode();
		String sessionId = Context.getRequestContext().getSessionId();
		Long userId = operator.getId();
        String userName = operator.getUserName();
		String token = TokenUtil.createToken(sessionId, userId);
        
        // 注册到在线用户库
		OnlineUserManagerFactory.getManager().register(token, appCode, sessionId, userId, userName);
		return new IdentityCard(token, operator);
	}
	

	/*
	 *  产生一个登录随机数给客户端，客户端使用该随机数对账号和密码进行加密后再传回后台
	 */
	public Object before(User user, XmlHttpEncoder encoder, HttpSession session) {
        int randomKey = MathUtil.randomInt(10000);
        encoder.put(SSOConstants.RANDOM_KEY, randomKey);
        session.setAttribute(SSOConstants.RANDOM_KEY, randomKey);
        
        return randomKey;
	}

	/**
	 * <p>
	 * 验证用户是否合法，如果合法，返回相应的operator对象，否则返回Null。
	 * 验证过程中任务业务逻辑错误，需要通知用户的，都必须抛出UserIdentificationException异常
	 * </p>
	 *
	 * @return
	 */
	protected abstract IOperator validate() throws UserIdentificationException;
	
	protected void loginSuccess(String msg) {
		HttpSession session = Context.getRequestContext().getSession();
		session.setAttribute(SSOConstants.LOGIN_MSG, msg);
	}
}
