/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tssx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;

import com.boubei.tss.AbstractTest4;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.record.Record;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.dm.record._Recorder;
import com.boubei.tss.dm.report.Report;
import com.boubei.tss.dm.report.ReportService;
import com.boubei.tss.dm.report._Reporter;
import com.boubei.tss.framework.sso.Anonymous;
import com.boubei.tss.framework.web.servlet.AfterUpload;
import com.boubei.tss.modules.cloud.entity.ModuleDef;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.entity.Role;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.permission.PermissionService;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.URLUtil;


public class XModuleTest extends AbstractTest4 {

	@Autowired XModule xModule;
	
	@Autowired RecordService recordService;
	@Autowired ReportService reportService;
	@Autowired PermissionService permissionService;
	
	@Autowired _Recorder recorder;
	@Autowired _Reporter reporter;
	
	static String UPLOAD_PATH = FileHelper.ioTmpDir() + "/upload/record/";
	String domain = "BD";
	
	 protected void init() {
    	super.init();
 
    	Report group1 = new Report();
    	group1.setName("我的报表");
    	group1.setType(Report.TYPE0);
    	group1.setParentId(Report.DEFAULT_PARENT_ID);
    	reportService.createReport(group1);
    	reportService.startOrStop(group1.getId(), 0);
    	
    	Record group2 = new Record();
    	group2.setName("我的功能");
    	group2.setType(Report.TYPE0);
    	group2.setParentId(Report.DEFAULT_PARENT_ID);
    	recordService.createRecord(group2);
    	recordService.startOrStop(group2.getId(), 0);
    }
 
	@Test
	public void testExportModule() {
		Logger.getLogger("com.boubei").setLevel(Level.INFO);
		
        // 新建角色
        Role role1 = createRole("部门主管", "-1");
        Role role2 = createRole("人事主管",  "-1");
        Role role3 = createRole("CEO",  "-1");
        
        Map<String, String> context = new HashMap<String, String>();
        context.put("role1", role1.getId().toString());
        context.put("role2", role2.getId().toString());
        context.put("role3", role3.getId().toString());
        
        Record recordGroup0 = new Record();
        recordGroup0.setType(Record.TYPE0);
        recordGroup0.setParentId(Record.DEFAULT_PARENT_ID);
        recordGroup0.setName("我的功能");
        recordService.createRecord(recordGroup0);
        
        Record recordGroup = new Record();
        recordGroup.setType(Record.TYPE0);
        recordGroup.setParentId(recordGroup0.getId());
        recordGroup.setName("record-group-1-x");
        recordService.createRecord(recordGroup);
        
		String tblDefine = "[ " +
				"	{'label':'申请人', 'code':'applier', 'type':'string'}," +
        		"	{'label':'日期', 'code':'fromDay', 'type':'date'}," +
        		"	{'label':'天数', 'code':'days', 'type':'number'}," +
        		"	{'label':'状态', 'code':'state'}," +
        		"   {'label':'实请天数','code':'wf_realdays','type':'number'}" +
        		"]";
		
		Record record1 = new Record();
		record1.setName("请假提报");
		record1.setType(Record.TYPE1);
		record1.setParentId(recordGroup.getId());
		record1.setDatasource(DMConstants.LOCAL_CONN_POOL);
		record1.setTable("tbl_qj_123");
		record1.setDefine(tblDefine);
		
		recordService.createRecord(record1);
		
		Report reportGroup0 = new Report();
		reportGroup0.setType(Report.TYPE0);
		reportGroup0.setParentId(Report.DEFAULT_PARENT_ID);
		reportGroup0.setName("report-group-1");
        reportService.createReport( reportGroup0 );
        
		Report reportGroup = new Report();
        reportGroup.setType(Report.TYPE0);
        reportGroup.setParentId( reportGroup0.getId() );
        reportGroup.setName("report-group-1");
        reportService.createReport(reportGroup);
		
        Report report1 = new Report();
        report1.setType(Report.TYPE1);
        report1.setParentId( reportGroup.getId() );
        report1.setName("报表X");
        report1.setScript(" select id, name from dm_report");
        report1.setParam("[]");
        reportService.createReport(report1);
        
		// 授权给角色，审批角色需要有浏览权限
		String permissions = role1.getId() + "|01100, " +role2.getId()+ "|01100, " +role3.getId()+ "|01100, " +Anonymous._ID+ "|00100";
        permissionService.saveResource2Roles("tss", Record.RESOURCE_TYPE, record1.getId(), "1", permissions);
        
        permissions = role1.getId() + "|0110, " +role2.getId()+ "|0110, " +role3.getId()+ "|0110, " +Anonymous._ID+ "|0010";
        permissionService.saveResource2Roles("tss", Report.RESOURCE_TYPE, report1.getId(), "1", permissions);
        
        List<Long> roleIds = new ArrayList<Long>();
        roleIds.add(role1.getId());
        roleIds.add(role2.getId());
        roleIds.add(role3.getId());
        
        ModuleDef module = new ModuleDef();
		module.setId(null);
		module.setKind("记账,进销存");
		module.setModule("工作台");
		module.setRoles(EasyUtils.list2Str(roleIds));
		module.setStatus("opened");
		module.setDescription("简介");
		commonDao.createObject(module);
		Long moduleId = module.getId();
		
		xModule.export(response, moduleId);
		
		module.setResource("pages/wms");
		commonDao.update(module);
		xModule.export(new MockHttpServletResponse(), moduleId);
	}
	
	
	AfterUpload upload = new XModule();
    HttpServletRequest mockRequest;
 
	@Test
	public void testImportModuleZip() {
		createDeveloper("kfz_1");
		
	    try {
	    	String filepath = URLUtil.getResourceFileUrl("/testdata/module1.zip").getFile();;
	    	String filename = "module1.zip";
			 
			upload.processUploadFile(mockRequest, filepath, filename);
			
			Assert.assertEquals(1, commonDao.getEntities("select name from Record where table=?", "tbl_qj_123").size() );
		} 
	    catch (Exception e) {
			log.error(e.getMessage(), e);
			Assert.assertFalse(e.getMessage(), true);
		}
    }
	
	private void createDeveloper(String userCode) {
		// 注册为开发者才能导入模块
		User u = new User();
        u.setLoginName(userCode);
        u.setUserName(userCode + "_cn");
        u.setPassword("123456");
        u.setGroupId(UMConstants.DEV_GROUP_ID);
		userService.regDeveloper(u);
		
		super.logout();
		super.login(u.getId(), u.getLoginName());

	}
	
	@Test
	public void testImportModuleJson() {
		createDeveloper("kfz_2");
		
	    try {
	    	String filepath = URLUtil.getResourceFileUrl("/testdata/module1.json").getFile();;
	    	String filename = "module1.json";
			 
			upload.processUploadFile(mockRequest, filepath, filename);
			
			Assert.assertEquals(1, commonDao.getEntities("select name from Record where table=?", "tbl_qj_123").size() );
		} 
	    catch (Exception e) {
			log.error(e.getMessage(), e);
			Assert.assertFalse(e.getMessage(), true);
		}
    }
}
