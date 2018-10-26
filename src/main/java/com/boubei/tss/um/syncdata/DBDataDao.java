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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.boubei.tss.framework.Config;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.connpool._Connection;
import com.boubei.tss.um.helper.dto.GroupDTO;
import com.boubei.tss.um.helper.dto.UserDTO;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;

/** 
 * 从MySQL等数据库里同步用户组织信息
 */
public class DBDataDao implements IOutDataDao {
    
	// 要求SQL的字段别名 和 DTO里的属性名一致
    protected static String[] groupDtoPropertyNames = {"id", "parentId", "name", "description"};
    protected static String[] userDtoPropertyNames  = {"id", "groupId", "loginName", "password", "userName", "sex", "telephone", "email", "employeeNo", "authMethod", "disabled"};
    
    public List<?> getOtherGroups(Map<String, String> paramsMap, String sql, String groupId) {
        sql = sql.replaceAll(":groupId", groupId);
        
        Connection conn = getConnection(paramsMap);
        return getDtosBySQL(conn, sql, groupDtoPropertyNames, GroupDTO.class);
    }

    public List<?> getOtherUsers(Map<String, String> paramsMap, String sql, String groupId, Object...otherParams) {
        sql = sql.replaceAll(":groupId", groupId);
 
        Connection conn = getConnection(paramsMap);
        return getDtosBySQL(conn, sql, userDtoPropertyNames, UserDTO.class);
    }
 
    protected List<?> getDtosBySQL(Connection conn, String sql, String[] dtoPropertyNames, Class<?> clazz) {
        List<Object> items = new ArrayList<Object>();
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                Object dto = BeanUtil.newInstance(clazz);
                Map<String, String> attrsMap = new HashMap<String, String>();
                for(int i = 0; i < dtoPropertyNames.length; i++){
                    Object value = rs.getObject(dtoPropertyNames[i]);
                    attrsMap.put(dtoPropertyNames[i], value == null ? null : value.toString());
                }
                BeanUtil.setDataToBean(dto, attrsMap);
                items.add(dto);
            }
            
            rs.close();   
                
        } catch (SQLException e) {
            throw new BusinessException("数据查询错误！", e);
        } finally {
        	try { stmt.close(); } catch (SQLException e) { }
            try { conn.close(); } catch (SQLException e) { }
        }
        return items;
    }
 
    protected Connection getConnection(Map<String, String> map){
        
        String driver= map.get(SyncDataHelper.DRIVER);
        String url   = map.get(SyncDataHelper.URL);
        String user  = map.get(SyncDataHelper.USERNAME);
        String pwd   = map.get(SyncDataHelper.PASSWORD);
        
        driver = (String) EasyUtils.checkNull(driver, Config.getAttribute("db.connection.driver_class").trim());
		url    = (String) EasyUtils.checkNull(url, Config.getAttribute("db.connection.url").trim());
		user   = (String) EasyUtils.checkNull(user, Config.getAttribute("db.connection.username").trim());
		pwd    = (String) EasyUtils.checkNull(pwd, Config.getAttribute("db.connection.password").trim());

        return _Connection.openConnection(driver, url, user, pwd);
    }
}