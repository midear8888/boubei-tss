/* ==================================================================   
 * Created [2009-7-7] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.sso.online;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.boubei.tss.framework.Config;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.framework.sso.context.Context;

/** 
 * <p>
 * 在线用户管理对象工厂类
 * </p>
 */
public class OnlineUserManagerFactory {
	
    protected static IOnlineUserManager manager = null;

    private static final Log log = LogFactory.getLog(OnlineUserManagerFactory.class);

    public static IOnlineUserManager getManager() {
        if (manager == null) {
            String onlineManager = Config.getAttribute(SSOConstants.ONLINE_MANAGER);
        	manager = (IOnlineUserManager) Global.getBean(onlineManager);
            
            String currentAppCode = Context.getApplicationContext().getCurrentAppCode();
			log.info("应用【" + currentAppCode + "】里在线用户库【" + manager.getClass().getName() + "】初始化成功！");
        }
        
        return manager;
    }
}
