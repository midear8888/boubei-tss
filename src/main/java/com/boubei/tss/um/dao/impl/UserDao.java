/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.dao.impl;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.boubei.tss.EX;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.BaseDao;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.dao.IUserDao;
import com.boubei.tss.um.entity.GroupUser;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.util.EasyUtils;

@Repository("UserDao")
public class UserDao extends BaseDao<User> implements IUserDao {

    public UserDao() {
		super(User.class);
	}
    
    public User initUser(User obj) {
		return create(obj);
	}
	
	public User removeUser(User user) {
		delete(user);

        Long deletedUserId = user.getId();
        deleteAll(findUser2GroupByUserId(deletedUserId)); // 用户对组
        deleteAll(findRoleUserByUserId(deletedUserId));  // 用户对角色
		
		return user;
	}
	
	public void setLastLoginTime(Long userId) {
		try {
			if( Context.getRequestContext().isApiCall() ) return;
			
			User user = getEntity(userId);
			user.setLastLogonTime(new Date());
			user.setLogonCount( EasyUtils.obj2Int(user.getLogonCount()) + 1 );
			
			refreshEntity(user);
		} 
    	catch( Exception e ) { }
	}
	
    public void checkUserAccout(User user) {
    	boolean isNew = user.getId() == null;
    	
        String loginName = user.getLoginName();
		User existUser = this.getUserByAccount(loginName, false);
		if( existUser != null && ( isNew || !existUser.equals(user) ) ) {
            throw new BusinessException( EX.parse(EX.U_29, loginName) );
        }
		
        String eamil = user.getEmail();
        if( !EasyUtils.isNullOrEmpty(eamil) ) {
        	existUser = this.getUserByAccount(eamil, false);
        	if( existUser != null && ( isNew || !existUser.equals(user) ) ) {
        		throw new BusinessException( EX.parse(EX.U_30, eamil) );
        	}
        }
        
        String mobile = user.getTelephone();
        if( !EasyUtils.isNullOrEmpty(mobile) ) {
        	existUser = this.getUserByAccount(mobile, false);
        	if( existUser != null && ( isNew || !existUser.equals(user) ) ) {
        		throw new BusinessException( EX.parse(EX.U_31, mobile));
        	}
        }
    }
 
	public User getUserByAccount(String account, boolean vaildate) {
	    List<?> users = getEntities("from User o where o.loginName = ? ", account);
	    if( users.isEmpty() ) {
	    	users = getEntities("from User o where ? in (o.telephone, o.email) ", account);
	    }
	    
        User user = users.size() > 0 ? (User) users.get(0) : null;
        
        if(vaildate) {
        	if (user == null) {
                throw new BusinessException(EX.U_00 + account);
            } 
            else if (ParamConstants.TRUE.equals(user.getDisabled())) {
                throw new BusinessException(EX.U_26);
            } 
            else {
    			Date accountLife = user.getAccountLife();
    			if (accountLife !=  null && new Date().after(accountLife) ) {
    			    throw new BusinessException(EX.U_27);
    			}
    		}
        }
        
        return user;
	}

	public List<?> findUser2GroupByUserId(Long userId) {
		return getEntities("from GroupUser o where o.userId = ? ", userId);
	}

	public List<?> findRoleUserByUserId(Long userId) {
		return getEntities("from RoleUser o where o.id.userId = ? and o.strategyId is null", userId);
	}

	public List<?> findRolesByUserId(Long userId) {
		String hql = "select distinct r from RoleUser ru, Role r where ru.id.roleId = r.id and ru.id.userId = ? and ru.strategyId is null ";
		return getEntities(hql, userId);
	}
 
	public GroupUser getGroup2User(Long groupId, Long userId) {
        List<?> list = getEntities("from GroupUser o where o.groupId = ? and o.userId = ?", groupId, userId);
		return !list.isEmpty() ? (GroupUser)list.get(0) : null;
	}
}