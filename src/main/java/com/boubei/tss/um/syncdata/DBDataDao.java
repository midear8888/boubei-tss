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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.um.helper.dto.GroupDTO;
import com.boubei.tss.um.helper.dto.UserDTO;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;

/** 
 * 从MySQL等数据库里同步用户组织信息
 */
public class DBDataDao implements IOutDataDao {
    
	// 要求SQL的字段别名 和 DTO里的属性名一致
    protected static String[] groupDtoFields = {"id", "parentId", "name", "description"};
    protected static String[] userDtoFields  = {"id", "groupId", "loginName", "password", "userName", "sex", "telephone", "email", "employeeNo", "authMethod", "disabled"};
    
    public List<?> getOtherGroups(Map<String, String> paramsMap, String sql, String groupId) {
        sql = sql.replaceAll(":groupId", groupId);
        return getDtosBySQL(paramsMap, sql, groupDtoFields, GroupDTO.class);
    }

    public List<?> getOtherUsers(Map<String, String> paramsMap, String sql, String groupId, Object...otherParams) {
        sql = sql.replaceAll(":groupId", groupId);
        return getDtosBySQL(paramsMap, sql, userDtoFields, UserDTO.class);
    }
 
    protected List<?> getDtosBySQL(Map<String, String> paramsMap, String sql, String[] fields, Class<?> clazz) {
        List<Object> items = new ArrayList<Object>();
        
        String datasource = paramsMap.get("ds");
        datasource = (String) EasyUtils.checkNull(datasource, DMConstants.LOCAL_CONN_POOL);
        List<Map<String, Object>> result = SQLExcutor.query(datasource, sql);
        for(Map<String, Object> row : result) {
        	Map<String, String> attrsMap = new HashMap<String, String>();
            for(String field : fields) {
				Object value = row.get(field.toLowerCase());
                attrsMap.put(field, value == null ? null : value.toString());
            }
            
            Object dto = BeanUtil.newInstance(clazz);
            BeanUtil.setDataToBean(dto, attrsMap);
            items.add(dto);
        }
        
        return items;
    }
}