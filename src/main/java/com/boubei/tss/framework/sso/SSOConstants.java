/* ==================================================================   
 * Created [2009-7-7] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.sso;

/** 
 * <p> 单点登录框架相关的一些属性命名 </p> 
 * 
 */
public interface SSOConstants {
    
    /** 配置文件中在线用户管理类属性名 */
    public static final String      ONLINE_MANAGER = "bean.onlineManager";

    /** 配置文件中用户登录自定义器属性名 */
    public static final String    LOGIN_COSTOMIZER = "class.name.LoginCostomizer";

    /**  配置文件中多应用信息存储类属性名  */
    public static final String    APPSERVER_STORER = "class.name.AppServerStorer";

    /** 身份转换对象实现类类名 */
    public static final String IDENTITY_GETTER = "class.name.IdentityGetter";

    /** 存在session里的用户权限（角色）名称  */
    public static final String USER_RIGHTS_L = "userRoles";   // List<Long id>
    public static final String USER_RIGHTS_S = "_userRoles";  // 逗号分隔字符串
    
    public static final String USER_MODULE_C = "userModules";   // List<>
    public static final String USER_MODULE_N = "_userModuleNames";  // List<>
    
    public static final String USER_ROLES_L = "userRoleNames";    // List<String name>
    public static final String USER_ROLES_S = "_userRoleNames";  // 逗号分隔字符串
    
    /** 存在session里的用户账号名称 */
    public static final String USER_ACCOUNT = "loginName";
    public static final String USER_CODE    = "userCode";
    public static final String USER_NAME    = "userName";
    public static final String USER_ID      = "userId";
    public static final String USER_PASSWORD= "password";
    public static final String USER_GROUP   = "userGroup";
    public static final String USER_GROUP_ID= "userGroupId";
    
    public static final String USER_DOMAIN  = "DOMAIN";
    public static final String USERS_OF_DOMAIN  = "USERS_OF_DOMAIN";
    public static final String USERIDS_OF_DOMAIN  = "USERIDS_OF_DOMAIN";
    
    public static final String RANDOM_KEY  = "randomKey";
    
    public static final String LOGIN_CHECK_KEY = "loginCheckKey";
}
