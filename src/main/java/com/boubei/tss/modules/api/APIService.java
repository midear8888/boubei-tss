package com.boubei.tss.modules.api;

import java.util.List;

import com.boubei.tss.um.entity.User;

public interface APIService {
	
	User getUserByCode(String userCode);
	
	/**
     * 获取指定用户在当前系统拥有的令牌（API、SSO）列表，包括授权给本人及匿名用户的令牌
     * @param uName
     * @param resource ID|Name|Sys
     * @param type Record|Report|SSO
     * @return
     */
    List<String> searchTokes(String uName, String resource, String type);
    
    /**
     * 模拟用户登录，初始化 Environment
     */
    String mockLogin(String userCode, String uToken);

}
