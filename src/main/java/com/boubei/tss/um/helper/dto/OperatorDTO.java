/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.helper.dto;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.boubei.tss.framework.persistence.IEntity;
import com.boubei.tss.framework.sso.Anonymous;
import com.boubei.tss.framework.sso.IPWDOperator;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.util.BeanUtil;

/**
 * 操作用户信息DTO
 */
public class OperatorDTO implements IPWDOperator, Serializable {
 
    private static final long serialVersionUID = 4239235668043689655L;
    
    public final static OperatorDTO ADMIN = 
    		new OperatorDTO(UMConstants.ADMIN_USER_ID, UMConstants.ADMIN_USER_NAME);

    private Long id;          // 用户ID 
    private String loginName; // 用户名: 即用户登陆系统的帐号
    private String userName;  // 姓名: 用户的实际姓名
    private String password;  // 密码
    
    public OperatorDTO(Long userId, String loginName) {
    	this.id = userId;
    	this.loginName = loginName;
    }
    
    public OperatorDTO(IEntity user) {
        // 共有的属性直接拷贝
        BeanUtil.copy(this, user);

        // 用户对象特有的其他属性全部放到DTO的map里面保存
        Map<String, Object> dtoMap = this.getAttributesMap();
        Field[] fields = user.getClass().getDeclaredFields();
        for (Field field : fields) {
            String fieldName = field.getName();
            try {
                dtoMap.put(fieldName, BeanUtil.getPropertyValue(user, fieldName));
            } catch (Exception e) {
            }
        }
    }
    
    public String toString() {
    	return id + ", " + loginName + ", " + userName;
    }

    /**
     * 保存USER对象的所有属性
     */
    private Map<String, Object> attributesMap = new HashMap<String, Object>();
    
    public Map<String, Object> getAttributesMap() {
        return attributesMap;
    }

    public Object getAttribute(String name) {
        return this.attributesMap.get(name);
    }

    public boolean isAnonymous() {
        return Anonymous.one.getId().equals(this.id);
    }
 
    public Long getId() {
        return id;
    }
 
    public void setId(Long id) {
        this.id = id;
    }
 
    public String getLoginName() {
        return loginName;
    }
 
    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }
 
    public String getPassword() {
        return password;
    }
 
    public void setPassword(String password) {
        this.password = password;
    }
 
    public String getUserName() {
        return userName;
    }
 
    public void setUserName(String userName) {
        this.userName = userName;
    }
}
