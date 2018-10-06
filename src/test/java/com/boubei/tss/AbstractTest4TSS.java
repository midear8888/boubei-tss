/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.transaction.TransactionConfiguration;

import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.persistence.ICommonDao;
import com.boubei.tss.framework.sso.Anonymous;
import com.boubei.tss.framework.sso.LoginCustomizerFactory;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.framework.sso.TokenUtil;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.modules.api.API;
import com.boubei.tss.modules.api.APIService;
import com.boubei.tss.modules.log.LogService;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.modules.param.ParamManager;
import com.boubei.tss.modules.param.ParamService;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.Role;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.permission.PermissionHelper;
import com.boubei.tss.um.permission.PermissionService;
import com.boubei.tss.um.service.IGroupService;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.um.service.IResourceService;
import com.boubei.tss.um.service.IRoleService;
import com.boubei.tss.um.service.IUserService;
import com.boubei.tss.um.sso.FetchPermissionAfterLogin;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.XMLDocUtil;

/**
 * Junit Test 类里执行构造函数的时候无事务，即构造函数不在单元测试方法的事物边界内。
 */
@ContextConfiguration(
        locations={
            "classpath:META-INF/spring-test.xml",  
            "classpath:META-INF/spring-framework.xml",  
            "classpath:META-INF/spring-um.xml",
            "classpath:META-INF/spring-dm.xml",
            "classpath:META-INF/spring-mvc.xml"
        } 
        , inheritLocations = false // 是否要继承父测试用例类中的 Spring 配置文件，默认为 true
      )
@TransactionConfiguration(defaultRollback = true) // 自动回滚，每个用力测试完成后自动清空产生的数据
public abstract class AbstractTest4TSS extends AbstractTransactionalJUnit4SpringContextTests { 
 
    protected Logger log = Logger.getLogger(this.getClass());    
    
    @Autowired protected IResourceService resourceService;
    @Autowired protected ILoginService loginSerivce;
    @Autowired protected APIService apiService;
    @Autowired protected PermissionService permissionService;
    @Autowired protected PermissionHelper permissionHelper;
    @Autowired protected LogService logService;
    @Autowired protected ParamService paramService;
    @Autowired protected ICommonDao commonDao;
    
    @Autowired protected IRoleService roleService;
    @Autowired protected IUserService userService;
    @Autowired protected IGroupService groupService;
    
    @Autowired protected API api;
    
    @Autowired protected H2DBServer dbserver;
 
    protected MockHttpServletResponse response;
    protected MockHttpServletRequest request;
    
	protected void initContext() {
		Global.setContext(super.applicationContext);
		
		Context.setResponse(response = new MockHttpServletResponse());
		
		request = new MockHttpServletRequest();
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SSOConstants.RANDOM_KEY, 100);
		request.setSession(session);
		
		Context.initRequestContext(request);
	}
    
    @Before
    public void setUp() throws Exception {
        initContext();
        init();
    }
    
    @After
    public void tearDown() throws Exception {
    }
 
    /**
     * 初始化CMS的动态属性相关模板
     */
    protected void init() {
    	/* 
    	 * 初始化数据库脚本。
    	 * 此处直接通过jdbc（ stmt.execute(sql) ）向H2插入了初始数据，没法在spring-test框架里自动回滚。
    	 * 通过hibernate生成的数据能回滚，因其事务由spring-test控制。
    	 */
    	if(paramService.getParam(0L) == null) {
			String sqlpath = _TestUtil.getInitSQLDir();
	    	log.info( " sql path : " + sqlpath);
	        _TestUtil.excuteSQL(sqlpath);
	        _TestUtil.excuteSQL(sqlpath + "/um");
    	}
    	
    	// 初始化虚拟登录用户信息
        login(UMConstants.ADMIN_USER_ID, UMConstants.ADMIN_USER_NAME);
        
        /* 初始化应用系统、资源、权限项 */
        Document doc = XMLDocUtil.createDocByAbsolutePath(_TestUtil.getSQLDir() + "/tss-resource-config.xml");
        resourceService.applicationResourceRegister(doc, UMConstants.PLATFORM_SYSTEM_APP);
    }
 
    protected void login(User user) {
    	login(user.getId(), user.getLoginName());
    }
    protected void login(Long userId, String loginName) {
    	LoginCustomizerFactory.customizer = null;
		if( paramService.getParam("class.name.LoginCostomizer") == null ) {
			ParamManager.addSimpleParam(ParamConstants.DEFAULT_PARENT_ID, "class.name.LoginCostomizer", "登录自定义", 
					FetchPermissionAfterLogin.class.getName());
		}
		
    	apiService.mockLogin(loginName);
    }   
    
    // 切换为匿名用户登陆，
    protected void logout() {
    	String token = TokenUtil.createToken("1234567890", UMConstants.ANONYMOUS_USER_ID);
    	Context.destroyIdentityCard(token);
    	login(UMConstants.ANONYMOUS_USER_ID, Anonymous.one.getLoginName()); 
    }
    
    protected Group createGroup(String name, Long parentId) {
    	return createGroup(name, parentId,  Group.MAIN_GROUP_TYPE );
    }
    protected Group createGroup(String name, Long parentId, int type) {
		Group g = new Group();
        g.setParentId(parentId);
        g.setName(name);
        g.setGroupType( type );
        groupService.createNewGroup(g , "", "");
        return g;
	}
    protected User createUser(String name, Long groupId, Long roleId) {
		User u = new User();
        u.setLoginName(name);
        u.setUserName(name + "_cn");
        u.setPassword("123456");
        u.setGroupId(groupId);
        userService.createOrUpdateUser(u , groupId+"", EasyUtils.obj2String(roleId));
        return u;
	}
    protected Role createRole(String name, Object userId) {
		Calendar calendar = new GregorianCalendar();
        calendar.add(UMConstants.ROLE_LIFE_TYPE, UMConstants.ROLE_LIFE_TIME);
        
		Role r = new Role();
        r.setIsGroup(0);
        r.setName(name);
        r.setParentId(UMConstants.ROLE_ROOT_ID);
        r.setStartDate(new Date());
        r.setEndDate(calendar.getTime());
        roleService.saveRole2UserAndRole2Group(r, EasyUtils.obj2String(userId), "");
        return r;
	}
}
