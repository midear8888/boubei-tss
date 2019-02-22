package com.boubei.tss.modules.api;

import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.PX;
import com.boubei.tss.framework.Config;
import com.boubei.tss.framework.sso.Anonymous;
import com.boubei.tss.framework.sso.IdentityCard;
import com.boubei.tss.framework.sso.LoginCustomizerFactory;
import com.boubei.tss.framework.sso.TokenUtil;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.framework.sso.context.RequestContext;
import com.boubei.tss.framework.sso.online.IOnlineUserManager;
import com.boubei.tss.framework.sso.online.OnlineUserManagerFactory;
import com.boubei.tss.framework.web.HttpClientUtil;
import com.boubei.tss.um.dao.IUserDao;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.helper.dto.OperatorDTO;
import com.boubei.tss.util.EasyUtils;

@Service("APIService")
public class APIServiceImpl implements APIService {
	
	Log log = LogFactory.getLog(this.getClass());
	
	@Autowired IUserDao userDao;
	
	public User getUserByCode(String userCode) {
		return userDao.getUserByAccount(userCode, true);
	}
	
	@SuppressWarnings("unchecked")
	public List<String> searchTokes(String uName, String resource, String type) {
		Date now = new Date();
		
		String hql = " select token from UserToken where user=? and resource=? and type=? and expireTime > ?";
		List<String> tokens = (List<String>) userDao.getEntities(hql, uName, resource, type, now );
		tokens.addAll( (List<String>) userDao.getEntities(hql, Anonymous._CODE, resource, type, now ) );
		
		/* 专门用于SSO的令牌，一个用户可以有多个（比如一个手机号通过多个小程序登录同一后台，每个小程序的openId都可登录）*/
		hql = " select token from UserToken where user=? and type='SSO' and expireTime > ?";
		tokens.addAll( (List<String>) userDao.getEntities(hql, uName, resource, type, now ) );
		
		/*
		 *  把用户的MD5密码也作为令牌，如果和uToken对的上，给予通过（适用于开放数据表链接给第三方用户录入，此时不宜逐个给用户发放令牌）
		 *  令牌校验通过后，对访问的数据服务、数据表接口等资源是否有相应的操作权限，还要在_Recorder和_Reporter里进一步校验。
		 *  自定义的接口 /api/*，需要在接口方法内，进行相应的角色和数据等控制
		 */
		User user = getUserByCode(uName);
		Object uToken = EasyUtils.checkNull(user.getAuthToken(), user.getPassword());
		tokens.add( (String) uToken );
		
		return tokens;
	}
	
    public String mockLogin(String userCode) {
    	User user = getUserByCode(userCode);
		Long userId = user.getId();
		String sessionId = Context.getRequestContext().getSessionId();
		sessionId = (String) EasyUtils.checkNull(sessionId, userCode); // WFJob等没有session，不适合用mockLogin
		
		// 设置令牌到Session，使Environment生效
		String token = TokenUtil.createToken(sessionId, userId);
		IdentityCard card = new IdentityCard(token, new OperatorDTO(user));
		Context.initIdentityInfo(card); 
		
		// saveUserRolesAfterLogin 及 设置session信息，获取用户域、组织、角色等信息
        LoginCustomizerFactory.instance().getCustomizer().execute();
        
        // 设置Cookie
        HttpClientUtil.setCookie(Context.getResponse(), RequestContext.USER_TOKEN, token);
        HttpClientUtil.setCookie(Context.getResponse(), RequestContext.JSESSIONID, sessionId);
        
        // 注册在线用户库
        IOnlineUserManager onlineUserManager = OnlineUserManagerFactory.getManager();
		String appCode = Config.getAttribute(PX.APPLICATION_CODE);
		onlineUserManager.register(token, appCode, sessionId, userId, user.getUserName());
        
		return token;
    }

}
