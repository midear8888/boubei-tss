/* ==================================================================   
 * Created [2016-3-5] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.dml;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.EX;
import com.boubei.tss.cache.Cacheable;
import com.boubei.tss.cache.JCache;
import com.boubei.tss.cache.Pool;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.modules.log.IBusinessLogger;
import com.boubei.tss.modules.log.Log;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.util.EasyUtils;

/**
 * 
	insert into tbl_jx(name,score,day,createtime,creator,version) values ('${name}', ${score}, '${day}', '${day}', 'Admin', 0)
	select IFNULL(max(id), 1) as maxid from tbl_jx
	update tbl_jx t set t.score = ${score} where t.id = ${maxid}
	delete from tbl_jx  where id = ${maxid} - 1
	
	var json = [];
	json.push({ "sqlCode": "s1", data: {"name": "JK", "score": 59, "day": "2017-01-01"} });
	json.push({ "sqlCode": "s1", data: {"name": "Jon", "score": 58, "day": "2017-01-02"} });
	json.push({ "sqlCode": "s2", data: {} });
	json.push({ "sqlCode": "s3", data: {"score": 100} });
	json.push({ "sqlCode": "s4", data: {} });
	
	var params = {};
	params.ds = "connectionpool";
	params.json = JSON.stringify(json);
	tssJS.post("/tss/auth/dml/multi", params, function(result) { console.log(result); } );
	
	Object {result: maxid: 2, "Success", step1: 1, step2: 1, step4: 1, step5: 0}
 */
@Controller
@RequestMapping( {"/auth/dml/", "/api/dml/"})
public class MultiSQLExcutor {
	
	Logger log = Logger.getLogger(this.getClass());
	
	public static final int PAGE_SIZE = 50;
	
	@Autowired public RecordService recordService;
	@Autowired private ILoginService loginService;
	  
    /**
     * 一个事务内执行新增、修改、删除、查询等多条SQL语句，All in one，并记录日志；要求都在一个数据源内执行。
     * 注：需严格控制数据行级权限； 在
     */
	@RequestMapping(value = "/multi", method = RequestMethod.POST)
    @ResponseBody
    public Object exeMultiSQLs(HttpServletRequest request, String ds) throws Exception {
		String json = DMUtil.getRequestMap(request, true).get("json");
    	return _exeMultiSQLs(json, ds, new HashMap<String, Object>());
    }
    	
	@SuppressWarnings("unchecked")
    public Object _exeMultiSQLs(String json, String ds, Map<String, Object> data) throws Exception {
    	
    	ICommonService commonService = Global.getCommonService();
 
    	ds = (String) EasyUtils.checkNull(ds, DMConstants.LOCAL_CONN_POOL);
    	Pool connpool = getDSPool(ds);
        Cacheable connItem = connpool.checkOut(0);
        Connection conn = (Connection) connItem.getValue();
        
        boolean autoCommit = true;
    	String sql = null;
    	List<Statement> statements = new ArrayList<Statement>();
    	
    	Map<String, Object> stepResults = new LinkedHashMap<String, Object>();
    	List<Log> logs = new ArrayList<Log>();
        try {
        	autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            
	    	List<Map<Object, Object>> list = new ObjectMapper().readValue(json, List.class);
	    	int index = 1;
	    	for(Map<Object, Object> item : list) {
	    		Long sqlId = EasyUtils.obj2Long(item.get("sqlID"));
	    		String sqlCode = (String) item.get("sqlCode");
	    		SQLDef sqlInfo;
	    		if( !EasyUtils.isNullOrEmpty(sqlCode) ) {
	    			List<?> temp = commonService.getList("from SQLDef where code=?", sqlCode);
	    			sqlInfo = (SQLDef) (temp.isEmpty() ? null : temp.get(0));
	    		} 
	    		else {
	    			sqlInfo = (SQLDef) commonService.getEntity(SQLDef.class, sqlId);
	    		}
	    		
	    		if(sqlInfo == null) {
	    			throw new BusinessException("code = " +sqlCode+ " or ID = " + sqlId + "'s SQLDef not exsit.");
	    		}
	    		
				sql = sqlInfo.getScript();
				
	    		Map<String, Object> params = (Map<String, Object>) item.get("data");
	    		if( params == null ) {
	    			params = new HashMap<String, Object>();
	    		}
	    		params.putAll(stepResults);
	    		params.putAll(data);
	    		
	    		String _sql = DMUtil.fmParse(sql, params, true).trim(); // MacrocodeCompiler.run(sql, params, true);
	    		if(_sql.startsWith("FM-parse-error")) {
	    			throw new BusinessException("SQL freemarker parse error" + _sql.replaceAll("FM-parse-error:", ""));
	    		}
	    		sql = _sql;
	    		
	    		if( sql.toLowerCase().startsWith("select") ) {
	    			List<Map<String, Object>> rows = SQLExcutor.query(ds, sql);
	    			if( !EasyUtils.isNullOrEmpty(rows) ) {
	    				stepResults.putAll( rows.get(0) );
	    			}
	    		} 
	    		else {
	    			log.debug(" excute  sql: " + sql);
	    			Statement statement = conn.createStatement();
	    			statements.add(statement);
                    statement.execute(sql);
                    stepResults.put("step" + index, statement.getUpdateCount());
	    		}
	    		
	    		Log excuteLog = new Log( sqlInfo.getCode(), sql ); // params.toString()
    			excuteLog.setOperateTable( "SQLDef_" + sqlInfo.getCode() );
    			logs.add(excuteLog);
    			
    			index++;
	    	}
	    	
	    	// 提交事务
	    	conn.commit();
	    	
	    	try {
	    		IBusinessLogger bLogger = (IBusinessLogger) Global.getBean("BusinessLogger");
	    		for(Log log : logs) {
					bLogger.output(log);
	    		}
    		} catch(Exception e) { }
        } 
        catch (Exception e) {
        	try { conn.rollback(); } catch (Exception e2) { }
        	
        	throw new BusinessException(e.getMessage() + " SQL= " + sql);
        } 
        finally {
        	try { conn.setAutoCommit(autoCommit); } catch (Exception e) { }
			try { 
				for(Statement statement : statements) {
					statement.close();
				}
			} catch (Exception e) { }
			
        	connpool.checkIn(connItem);
        }
	        
    	stepResults.put("result", "Success");
    	return stepResults;
    }
    
    private static Pool getDSPool(String datasource) {
    	Pool connpool = JCache.getInstance().getPool(datasource);
        if(connpool == null) {
        	throw new BusinessException( EX.parse(EX.DM_02, datasource) );
        }
        return connpool;
    }
}
