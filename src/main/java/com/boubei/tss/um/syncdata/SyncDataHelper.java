/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.syncdata;

import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.helper.dto.UserDTO;
import com.boubei.tss.um.sso.UMPasswordIdentifier;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;

public class SyncDataHelper {
    
	public final static String DRIVER = "driver";
    public final static String URL = "url";
    public final static String USERNAME = "user";
    public final static String PASSWORD = "password";
    
    public final static String QUERY_GROUP_SQL_NAME = "groupSql";
    public final static String QUERY_USER_SQL_NAME  = "userSql";
 
    /**
     * <p>
     * 拷贝用户DTO到实体对象
     * 只拷贝部分基本属性
     * </p>
     * @param user
     * @param userDTO
     */
    public static void setUserByDTO(User user, UserDTO userDTO) {
    	String loginName = userDTO.getLoginName();
		user.setLoginName(loginName);
    	user.setUserName(userDTO.getUserName());
        user.setDisabled(userDTO.getDisabled());
        user.setAccountLife(userDTO.getAccountLife());
        user.setEmail(userDTO.getEmail());
        user.setSex(userDTO.getSex());
        user.setTelephone(userDTO.getTelephone());
        user.setEmployeeNo(userDTO.getEmployeeNo());
        user.setOrigin("Job同步");
        
        if( !EasyUtils.isNullOrEmpty(userDTO.getAuthMethod()) ) {
        	user.setAuthMethod(userDTO.getAuthMethod());
        } else {
        	user.setAuthMethod(UMPasswordIdentifier.class.getName());
        }
        
        String userPassword = userDTO.getPassword();
        if(userPassword == null) {
        	userPassword = loginName; // 默认：密码 = 登陆账号
        }
        if(userPassword.length() < 32) { // 如果是32位，则同步的是已经加密好的密码
        	userPassword = user.encodePassword(userPassword);
        }
        user.setPassword(userPassword);
        user.setPasswordQuestion("?");
        user.setPasswordAnswer( System.currentTimeMillis() + "!" );
        
        user.setFromUserId(userDTO.getId());
    }
    
    public static IOutDataDao getOutDataDao(String dsType) {
        dsType = (String) EasyUtils.checkNull(dsType, DBDataDao.class.getName());
        return (IOutDataDao) BeanUtil.newInstanceByName(dsType);
    }
    
    // 安全监测
    public static void checkSecurity(Group group, UserDTO userDto) {
    	
    	// 1、检测用户是否跨域同步（防止员工管理里，用户恶意修改所属组为其它域的组，然后把账号同步到了其它域下）
		String domain = userDto.getDomain();
		if( !EasyUtils.isNullOrEmpty(domain) && !domain.equals(group.getDomain()) ) {
			throw new BusinessException("非法同步，指定域下的用户不同同步至其它域。"
					+ "group = " + group + ", user=" +  EasyUtils.obj2Json(userDto), true);
		};
    }
}

