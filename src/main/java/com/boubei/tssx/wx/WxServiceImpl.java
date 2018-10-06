package com.boubei.tssx.wx;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.dao.IGroupDao;
import com.boubei.tss.um.dao.IUserDao;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.GroupUser;
import com.boubei.tss.um.entity.RoleUser;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.service.IGroupService;
import com.boubei.tss.util.EasyUtils;

@Service("WxService")
public class WxServiceImpl implements WxService {
	
	Logger log = Logger.getLogger(this.getClass());
	
	@Autowired IUserDao userDao;
	@Autowired IGroupDao groupDao;
	@Autowired IGroupService groupService;

	public User getUserByAuthToken(String authToken) {
		String sql = "from User where authToken = ? order by id desc";
    	List<?> users = userDao.getEntities(sql, authToken);
    	return users.size() > 0 ? (User) users.get(0) : null;
	}
	
	public User checkPhoneNum(String phoneNum) {
    	return userDao.getUserByAccount(phoneNum, false);
	}
	
	/* 
	 * 创建一个用户，并且设置用户与注册目标组之间的关系
	 * 写在Service里保证事务完整性 
	 */
	public String regWxUser(User user, String domain, String _group) {
		
		log.debug(domain + ", " + _group ); 
		 
		// domain 可以是域组的ID
		try {
			Long domainGroupId = Long.valueOf(domain);
			Group g = groupDao.getEntity(domainGroupId);
			domain = g.getDomain();
		} 
		catch(Exception e) { }
		
		// groupName 可以是组的ID
		try {
			Long groupId = Long.valueOf(_group);
			Group g = groupDao.getEntity(groupId);
			_group = g.getName();
		} 
		catch(Exception e) { }
		
		_group = (String) EasyUtils.checkNull(_group, domain);
        List<?> groups = userDao.getEntities("from Group where domain = ? and name = ? order by decode asc", domain, _group);
        if(groups.isEmpty()) {
        	return WXUtil.returnCode(401, domain, _group);
        }
        
		userDao.create(user);
        
		Group group = (Group) groups.get(0);
        GroupUser gu = new GroupUser(user.getId(), group.getId());
        userDao.createObject(gu);
        
        return WXUtil.returnCode(100);  // register user success;
	}
	
	public String regWxBusiness(User user, String domain) {
		Group domainGroup = groupService.createDomainGroup(domain);
    	user.setGroupId(domainGroup.getId());
    	
		this.regWxUser(user, domain, domainGroup.getId().toString());
		
        // 商家默认授予“域管理员”角色
 		RoleUser ru = new RoleUser();
 		ru.setRoleId(UMConstants.DOMAIN_ROLE_ID);
 		ru.setUserId(user.getId());
 		userDao.createObject(ru);
 		
 		return WXUtil.returnCode(100); // register business user success;
	}

	public void bindOpenID(User user, String openID) {
		user.setAuthToken(openID);
		userDao.update(user);
	}
	
	public User getBelongUser(String belong) {
		if( WXUtil.isNull(belong) ) return null;
		 
		List<?> users = userDao.getEntities("from User o where ? in (o.id, o.loginName) ", belong);
	    if( users.isEmpty() ) {
	    	users = userDao.getEntities("from User o where ? in (o.telephone, o.email) ", belong);
	    }
	    
        if( users.size() > 0 ) {
        	User user = (User) users.get(0);
        	
        	// 获取用户所在组和域
        	List<?> groups = userDao.getEntities("select g from Group g, GroupUser gu where g.id=gu.groupId and gu.userId=? and g.groupType=1 ", user.getId());
        	if( !groups.isEmpty() ) {
        		Group g = (Group) groups.get(0);
        		user.setGroupId(g.getId());
        		user.setGroupName(g.getName());
        		user.setDomain(g.getDomain());
        	}
        	
        	return user;
        }
        return null;
	}

}
