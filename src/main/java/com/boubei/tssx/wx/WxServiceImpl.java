package com.boubei.tssx.wx;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.framework.persistence.ICommonDao;
import com.boubei.tss.um.dao.IGroupDao;
import com.boubei.tss.um.dao.IUserDao;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.GroupUser;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.entity.UserToken;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tssx.wx.gzh.GZHPhone;

@Service("WxService")
public class WxServiceImpl implements WxService {
	
	Logger log = Logger.getLogger(this.getClass());
	
	@Autowired IUserDao userDao;
	@Autowired IGroupDao groupDao;
	@Autowired ICommonDao commonDao;

	public User getUserByAuthToken(String authToken) {
		String sql = "from User where authToken = ? order by id desc";
    	List<?> users = userDao.getEntities(sql, authToken);
    	if( users.size() > 0 ) {
    		return (User)users.get(0);
    	}
    	
    	sql = "select u from User u, UserToken ut where u.loginName = ut.user and ut.type = 'SSO' and ut.token = ? order by u.id desc";
    	users = userDao.getEntities(sql, authToken);
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

	public void bindOpenID(User user, String openID) {
		if(user.getAuthToken() == null) {
			user.setAuthToken(openID);
			userDao.update(user);
		} 
		else {
			String userCode = user.getLoginName();
			UserToken ut = new UserToken();
			ut.setCreator(userCode);
			ut.setUser(userCode);
			ut.setToken(openID);
			ut.setResource("WX_OPEN_ID");
			ut.setType("SSO");
			ut.setExpireTime( DateUtil.parse("2099-12-31") );
			userDao.createObject(ut);
		}
		
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
	
	@SuppressWarnings("unchecked")
	public String sendWxGZHMsg(Map<String, String> requestMap) throws IOException {
		
		String accessToken = null;
		try {
			accessToken = new WXUtil().getToken( requestMap.get("appid"));
    	} catch (Exception e) {
    		return "{\"code\": \"fail\", \"errorMsg\": \"获取accessToken接口失败 " + e.getMessage() + "\"}";
    	}
		
		String url = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=" + accessToken;
		PostMethod postMethod = new PostMethod(url);
		postMethod.setRequestHeader("Content-Type", "application/json");
		
		Map<String, Object> da = new ObjectMapper().readValue( requestMap.get("data"), Map.class); 
		JSONObject data = new JSONObject();
		
		for (String in : da.keySet()) {
			Map<String, String> va = (Map<String, String>) da.get(in);
			JSONObject keyvalue = new JSONObject();
			for (String k : va.keySet()) {
				String str = va.get(k);
				keyvalue.put(k, str);
			}
			data.put(in, keyvalue);
		}
		
		String miniprogram_ = (String) EasyUtils.checkNull(requestMap.get("miniprogram"), "{}");
		Map<String, Object> mp = new ObjectMapper().readValue( miniprogram_, Map.class); 
		JSONObject miniprogram = new JSONObject();
		
		for (String in : mp.keySet()) {
			String keyvalue = (String) mp.get(in);
			miniprogram.put(in, keyvalue);
		}
		
		String mobile = requestMap.get("phone");
		String appid = requestMap.get("appid");
		String hql = "from GZHPhone where mobile = '" + mobile + "' and appid = '" + appid + "'";
		List<GZHPhone> bindPhones = (List<GZHPhone>) commonDao.getEntities(hql);
		
		JSONObject json = new JSONObject();
		if(bindPhones.size() == 0) {
			json.put("touser", requestMap.get("unionid")); // 适用于小程序与公众号拥有共同unionid的情况
		} else {
			json.put("touser", bindPhones.get(0).getOpenid());
		}
		json.put("template_id", requestMap.get("template_id"));
		json.put("url", requestMap.get("url"));
		json.put("miniprogram", miniprogram);
		json.put("data", data);
		
		String requestData = json.toString();
		byte[] b = requestData.getBytes("UTF-8");
        InputStream is = new ByteArrayInputStream(b, 0, b.length);
        RequestEntity re = new InputStreamRequestEntity(is, b.length, "application/json; charset=UTF-8");
		postMethod.setRequestEntity(re);
		
		HttpClient httpClient = new HttpClient();
		int statusCode = httpClient.executeMethod(postMethod);
		if (statusCode != 200) {
			return  "{\"code\": \"fail\", \"errorMsg\": \"发送post请求失败\"}";
		}
		
		String responseBody = postMethod.getResponseBodyAsString();
		Map<String, String> result = new ObjectMapper().readValue(responseBody, Map.class);
		
		if(result.get("errmsg").equals("ok")){
			return "{\"code\": \"success\", \"data\": \"OK\"}";
		}
		else{
			return "{\"code\": \"fail\", \"errorMsg\": \"" + result.get("errmsg") + "\"}";
		}
	}

}
