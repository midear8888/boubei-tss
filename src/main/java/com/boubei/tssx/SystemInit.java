package com.boubei.tssx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.modules.param.Param;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.modules.param.ParamManager;
import com.boubei.tss.modules.param.ParamService;

@Controller
@RequestMapping("/init")
public class SystemInit {
	
	/**
	 * http://localhost:9000/tss/init/domain
	 * 
	 * 测试录入表domain字段：索引（domain查询索引、唯一性约束联合索引）、数据库兼容性、跨数据源的情形（补数据是否正常）
	 * 
	 * 自动给所有录入表补上domain字段及值 
	 * update j_order t set t.domain = 
	   		(select domain from um_group g, um_user u, um_groupuser gu where g.id=gu.groupid and gu.userId = u.id and u.loginName = t.creator)
	   	 where t.id > 0;
	 */
	@RequestMapping(value = "/domain", method = RequestMethod.GET)
	@ResponseBody
	public void fixDataDomain() {
		List<Map<String, Object>> users = SQLExcutor.query(DMConstants.LOCAL_CONN_POOL, 
				"select u.loginName, g.domain from um_group g, um_user u, um_groupuser gu where g.id=gu.groupid and gu.userId = u.id");
		
		Map<String, String> m1 = new HashMap<String, String>();
		for(Map<String, Object> m : users) {
			String loginName = (String) m.get("loginname");
			String domain = (String) m.get("domain");
			m1.put(loginName, domain);
		}
		
		List<Map<String, Object>> list = SQLExcutor.query(DMConstants.LOCAL_CONN_POOL, 
				"select distinct datasource, rctable from dm_record where rctable is not null and type=1");
		
		for(Map<String, Object> m : list) {
			String datasource = (String) m.get("datasource");
			String table = (String) m.get("rctable");
			
			try {
				SQLExcutor.excute("alter table " +table+ " add column domain varchar(50)", datasource);
			} 
			catch(Exception e) { }
			
			try {
				// table 不一定存在
				List<Map<String, Object>> data = SQLExcutor.query(datasource, "select id, creator from " +table);
				for(Map<String, Object> item : data) {
					Object itemId = item.get("id");
					String creator = (String) item.get("creator");
					String domain = m1.get(creator);
					
					SQLExcutor.excute("update " +table+ " set domain = '" + domain + "' where id = " + itemId, datasource);
				}
			} 
			catch(Exception e) { 
				e.printStackTrace();
			}
		}
	}
	
	@Autowired protected ParamService paramService;
	
	@RequestMapping(value = "/param", method = RequestMethod.GET)
	@ResponseBody
	public Object init() {
		String[][] items = new String[][]{ 
        		{"1", "停用"}, 
        		{"0", "启用"} 
        	};
        addComboParam("EntityState", "对象状态", items);
		
		items = new String[][]{ 
				{ "1", "超级管理员"},
	        	{ "2", "管理用户"},
	        	{ "3", "实操用户"}
        	};
        addComboParam("UserType", "用户类型", items);
		
		return new Object[] { "Success" };
	}
	
	void addComboParam(String code, String name, String[][] items) {
		Param cp;
		List<Param> list;
		
		if( (cp = paramService.getParam(code)) != null) {
			list = paramService.getComboParam(code);
		}
		else {
			cp = ParamManager.addComboParam(ParamConstants.DEFAULT_PARENT_ID, code, name);
			list = new ArrayList<Param>();
		}
		
		L:for(String[] item : items) {
			for(Param p : list) {
				if(p.getValue().equals(item[0])) {
					p.setText(item[1]);
					paramService.saveParam(p);
					continue L;
				}
			}
			ParamManager.addParamItem(cp.getId(), item[0], item[1], ParamConstants.COMBO_PARAM_MODE);
		}
	}

}
