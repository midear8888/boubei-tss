package com.boubei.tss.modules.api;

import java.util.List;
import java.util.Map;

import com.boubei.tss.cache.aop.Cached;
import com.boubei.tss.cache.aop.QueryCached;
import com.boubei.tss.cache.extension.CacheLife;
import com.boubei.tss.dm.dml.SQLExcutor;
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
     * 模拟用户登录，初始化 Environment。
     */
    String mockLogin(String userCode);
    
    /**
     * 用于重启后自动登录，加QueryCached，防止重启后踩踏（多个Admin刷）
     */
    @QueryCached
    @Cached(cyclelife = CacheLife.SHORTER)
    String autoLogin(String userCode);
    
    boolean setRole4User(Map<String,String> requestMap, String userCode, String group, String roles);   

    @QueryCached
    @Cached(cyclelife = CacheLife.SHORTER)
    SQLExcutor queryByScript(String sqlCode, Map<String, String> requestMap, int maxPagesize, String tag, Object cacheFlag);
}
