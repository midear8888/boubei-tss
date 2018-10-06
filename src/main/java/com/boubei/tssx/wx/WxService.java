package com.boubei.tssx.wx;

import com.boubei.tss.um.entity.User;

public interface WxService {

	User getUserByAuthToken(String authToken);

	User checkPhoneNum(String phoneNum);

	String regWxUser(User user, String domain, String groupName);
	
	String regWxBusiness(User user, String domain);

	/**
	 * 绑定微信openId到同手机号用户
	 */
	void bindOpenID(User user, String openID);
	
	User getBelongUser(String belong);

}
