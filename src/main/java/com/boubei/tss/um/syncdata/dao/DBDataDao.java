/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.syncdata.dao;

import java.sql.Connection;
import java.util.Map;

import com.boubei.tss.framework.persistence.connpool._Connection;
import com.boubei.tss.um.syncdata.SyncDataHelper;

/** 
 * 从MySQL等数据库里同步用户组织信息
 */
public class DBDataDao extends BaseDBDataDao{
 
    protected Connection getConnection(Map<String, String> map){
        
        String driver= map.get(SyncDataHelper.DRIVER);
        String url   = map.get(SyncDataHelper.URL);
        String user  = map.get(SyncDataHelper.USERNAME);
        String pwd   = map.get(SyncDataHelper.PASSWORD);

        return _Connection.openConnection(driver, url, user, pwd);
    }
}