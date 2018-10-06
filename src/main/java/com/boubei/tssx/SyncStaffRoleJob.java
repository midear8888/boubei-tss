/* ==================================================================   
 * Created [2016-3-5] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tssx;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.Global;
import com.boubei.tss.modules.timer.AbstractJob;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.service.IUserService;
import com.boubei.tss.util.EasyUtils;

/**
 * 自动同步员工管理等模块维护的用户和角色对应关系到UM里。
 * 如果员工管理、客户管理等模块维护了账号的岗位，则角色信息以此维护的员工岗位为准。
 * 
 * 生成审批流参与人员时，需要用到人员的角色，所以不能再 AfterLogin 自定义时再根据人员岗位去动态加载角色，需要保持同步。
 * 
 * com.boubei.tssx.SyncStaffRoleJob | 0 12 * * * ? | 
 * 		select phone user, position role from staff_info where updatetime > date_sub(now(), interval 3 day) or createtime > date_sub(now(), interval 3 day)
 * 
 */
public class SyncStaffRoleJob extends AbstractJob {
	
	private String insertSQL = "insert into um_roleuser (userId, roleId) values(?, ?)";
	private String deleteSQL = "delete from um_roleuser where userId=? and roleId=? and strategyId is null";
	
	IUserService loginService = (IUserService) Global.getBean("UserService");
	
	/* 
	 * jobConfig的格式为 : sql 
	 */
	protected String excuteJob(String jobConfig, Long jobID) {
		log.info("开始同步员工角色信息......");
		
		if( EasyUtils.isNullOrEmpty(jobConfig) )  {
			String msg = "同步员工角色信息的配置信息为空。" + jobConfig;
			log.info(msg);
			return msg;
		}
		 
		String sql = jobConfig;
		String dataSource = DMConstants.LOCAL_CONN_POOL;
		
		List<Map<String, Object>> list = SQLExcutor.query(dataSource, sql);
		List<Object[]> addList = new ArrayList<Object[]>();
		List<Object[]> delList = new ArrayList<Object[]>();
		
		// 没有员工管理数据，则本JOB执行后不会有任何角色变动
		for(Map<String, Object> item : list) {
			String account = (String) item.get("user");
			User user = loginService.getUserByLoginName(account);
			if(user == null) continue;
			
			Long userId = user.getId();
			String[] _currRoles = EasyUtils.obj2String(item.get("role")).split(",");
			
			List<Long> currRoles = new ArrayList<Long>(); 
			List<Object> existRoles = getExistRoles(userId);
			for(String _currRole : _currRoles) {
				try {
					currRoles.add( EasyUtils.obj2Long(_currRole) );
				} catch(Exception e) { 
					// 可能有非数字的手动填写的异常岗位
				}
			}
			
			for(Long roleId : currRoles) {
				if( !existRoles.contains(roleId) ) {
					addList.add(new Object[]{ userId, roleId });
				}
			}
			
			for(Object roleId : existRoles) {
				if( !currRoles.contains(roleId) ) {
					delList.add(new Object[]{ userId, roleId });
				}
			}
		}
		
		if(addList.size() > 0) {
			SQLExcutor.excuteBatchII(insertSQL, addList, DMConstants.LOCAL_CONN_POOL);
		}
		if(delList.size() > 0 ) {
			SQLExcutor.excuteBatchII(deleteSQL, delList, DMConstants.LOCAL_CONN_POOL);
		}
		
		String returnMsg = "完成开始同步员工角色信息，新增用户角色关系" + addList.size() + "条， 删除" + delList.size() + "";
		log.info(returnMsg);
		return returnMsg;
	}
	
	List<Object> getExistRoles(Long user) {
		String sql = "select distinct roleId from um_roleuser where userId = ? and strategyId is null";
		List<Map<String, Object>> result = SQLExcutor.queryL(sql, user);
		return EasyUtils.attr2List(result, "roleid");
	}
	 
}
