/* ==================================================================   
 * Created [2016-3-5] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm;

import com.boubei.tss.PX;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.param.ParamConfig;
import com.boubei.tss.modules.param.ParamManager;

public final class DMConstants {
	
	public final static String LOCAL_CONN_POOL = "connectionpool";
	
	public final static String USER_ID_L    = "_userId";
	public final static String USER_ID_S    = "userId";
	public final static String USER_CODE    = "userCode";
	public final static String FROM_USER_ID = "fromUserId";
	
	public final static String RECORD_TABLE_LIST = "_TABLE_LIST";
	public final static String RECORD_EMPTY_TABLE = "dm_empty";
	
	// 常用宏: 数据表过滤条件里 加 1=1 <#if 部门经理??>${filterByGroup}</#if>
	public final static String FILTER_BY_GROUP = "filterByGroup";
	public final static String FILTER_BY_GROUP_DEEP = "filterByGroupDeep";
	public final static String GROUP_CONDITION = "<#if USERS_OF_GROUP??> and creator in (${USERS_OF_GROUP}) </#if>";
	public final static String GROUP_DEEP_CONDITION = "<#if USERS_OF_GROUP_DEEP??> and creator in (${USERS_OF_GROUP_DEEP}) </#if>";
	
	public final static String FILTER_BY_DOMAIN = "filterByDomain";
	public final static String DOMAIN_CONDITION_0 = "<#if USERS_OF_DOMAIN??> and creator in (${USERS_OF_DOMAIN}) </#if>";
	public final static String DOMAIN_CONDITION  = "<#if DOMAIN??> and domain = '${DOMAIN}' </#if>";
	public final static String CREATOR_CONDITION = "<#if userCode??> and creator = '${userCode}' </#if>";
	
	public static String RECORD_PORTLET_HTML = "/tss/modules/dm/recorder.html?id=";
	
	// XForm 模板
	public static final String XFORM_GROUP  = "template/dm/group_xform.xml";
	public static final String XFORM_REPORT = "template/dm/report_xform.xml";
	public static final String XFORM_RECORD = "template/dm/record_xform.xml";
    
    // Grid 模板
	public static final String GRID_RECORD_ATTACH = "template/dm/record_attach_grid.xml";
	
	// 数据表导入模板自定义
	public static final String IMPORT_TL_FIELDS  = "import_tl_fields";
	public static final String IMPORT_TL_IGNORES = "import_tl_ignores";
    
    //报表模板资源文件目录
	public static final String REPORT_TL_DIR_DEFAULT = "more/bi_template";
	public static final String REPORT_TL_TYPE = "reportTL";
	
	/**
	 * 如果是一个开发者角色的用户，则需要为其隔离出一个专有目录
	 */
	public static String getReportTLDir() {
		String tlDir = ParamConfig.getAttribute(PX.REPORT_TL_DIR, REPORT_TL_DIR_DEFAULT);
		if( Environment.isDeveloper() ) {
			tlDir += "/" + Environment.getUserCode();
		}
		return tlDir;
	}
	
	public static String getDS(String ds) {
		if( ds == null ) {
            try {
                return ParamManager.getValue(PX.DEFAULT_CONN_POOL).trim(); // 默认数据源
            } catch (Exception e) {
            }
        }
		return ds;
	}
}
