/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.sso.online;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.framework.persistence.ICommonDao;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.online.IOnlineUserManager;
import com.boubei.tss.util.EasyUtils;

/**
 * 在线用户库（数据库）
 */
@Service("DBOnlineUserService")
public class DBOnlineUserService implements IOnlineUserManager {
	
	@Autowired private ICommonDao dao;
	
    /*     
     * 如果在线用户库中没有相同的用户存在， 则在在线用户库中添加此记录。
     * 
     * 1、同一IP，同一用户，只能有一个登录session（开多个浏览器不被支持？）
     * 2、同一用户，只能在PC、微信、H5上分别登录一个session
     */
    public void register(String token, String appCode, String sessionId, Long userId, String userName) {
        List<?> list = queryExists(userId);
        if( list.isEmpty() ) {
        	DBOnlineUser ou = new DBOnlineUser(userId, sessionId, appCode, token, userName);
        	dao.create(ou);
        } else {
        	DBOnlineUser ou = (DBOnlineUser) list.get(0);
        	ou.setSessionId(sessionId);
        	ou.setLoginCount( EasyUtils.obj2Int(ou.getLoginCount()) + 1 );
        	dao.update(ou);
        }
    }
    
    private List<?> queryExists(Long userId) {
    	String clientIp = Environment.getClientIp(); // 如要控制同一人只能登录一个IP，则此处查询忽略IP
    	String origin = Environment.getOrigin();
    	
    	String hql = " from DBOnlineUser o where o.userId = ? and o.clientIp = ? and o.origin = ? ";
        return dao.getEntities(hql, userId, clientIp, origin);
    }

	public void logout(Long userId) {
		List<?> list = queryExists(userId);
		dao.deleteAll(list); // 只删除同一ip的登录信息
	}
 
    /*
     * session超时销毁调用。
     * 根据 SessionId，找到用户并将用户的sessionId置为Null，表示已经注销。
     */
    public String logout(String appCode, String sessionId) {
    	String hql = " from DBOnlineUser o where o.appCode = ? and o.sessionId = ? ";
        List<?> list = dao.getEntities(hql, appCode, sessionId);
        
        String token = null;
    	for(Object entity : list) {
    		DBOnlineUser ou = (DBOnlineUser) dao.delete(entity);
        	token = ou.getToken();
    	}
    	
    	// 将12小时前的在线信息删除（应该都是漏删除的了）
    	long nowLong = new Date().getTime(); 
        Date time = new Date(nowLong - (long) (12 * 60 * 60 * 1000)); 
    	dao.deleteAll(dao.getEntities("from DBOnlineUser o where o.loginTime < ?", time));
    	
		return token;
    }

    public boolean isOnline(String token) {
        List<?> list = dao.getEntities("from DBOnlineUser o where o.token = ? ", token);
		return list.size() > 0;
    }
}
