package com.boubei.tss;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.transaction.TransactionConfiguration;

import com.boubei.tss.cache.Cacheable;
import com.boubei.tss.cache.JCache;
import com.boubei.tss.cache.Pool;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.persistence.ICommonDao;
import com.boubei.tss.framework.sso.Anonymous;
import com.boubei.tss.framework.sso.LoginCustomizerFactory;
import com.boubei.tss.framework.sso.TokenUtil;
import com.boubei.tss.framework.sso.context.Context;
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
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.URLUtil;

@ContextConfiguration(
	  locations={
			"classpath:META-INF/spring-framework.xml",
			"classpath:META-INF/spring-um.xml",
		    "classpath:META-INF/spring-mvc.xml",
		    "classpath:META-INF/spring-test.xml"
	  }
) 
@TransactionConfiguration(defaultRollback = true) // 自动回滚设置为false，否则数据将不插进去
public abstract class AbstractTest4 extends AbstractTransactionalJUnit4SpringContextTests { 
 
    protected static Logger log = Logger.getLogger(AbstractTest4.class);    
    
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
    
    protected MockHttpServletRequest request;
    protected MockHttpServletResponse response;
    
    // 必须要先有个域登录
 	String domain = "BD";
 	User testUser;
    
    @Before
    public void setUp() throws Exception {
    	
        Global.setContext(super.applicationContext);
        Context.setResponse(response = new MockHttpServletResponse());
		Context.initRequestContext(request = new MockHttpServletRequest());
        
        init();
    }
    
    private void init() {
    	if( paramService.getAllParams(true).isEmpty() ) {
    		String sqlPath = URLUtil.getResourceFileUrl("sql/mysql").getPath();
    		excuteSQLFile(sqlPath);
    	}
    	
    	if( commonDao.getEntities("from Group where name=?", domain).isEmpty() ) {
	    	// 企业注册
			User domainUser = new User();
			domainUser.setLoginName(domain);
			domainUser.setUserName(domain);
			domainUser.setPassword("123456");
			userService.regBusiness(domainUser, domain);
			Group domainGroup = (Group) commonDao.getEntities("from Group where name=?", domain).get(0);
	
	        // 新增用户
			testUser = createUser("Mr.Test", domainGroup.getId(), -1L);
    	}
        
    	// 初始化虚拟登录用户信息
        login( testUser.getId(), testUser.getLoginName() );
    }
    
    protected void login(Long userId, String loginName) {
        LoginCustomizerFactory.customizer = null;
		if( paramService.getParam("class.name.LoginCostomizer") == null ) {
			ParamManager.addSimpleParam(ParamConstants.DEFAULT_PARENT_ID, "class.name.LoginCostomizer", "登录自定义", 
					FetchPermissionAfterLogin.class.getName());
		}
		
    	apiService.mockLogin(loginName, "1234567890");
    }
    
    // 切换为匿名用户登陆，
    protected void logout() {
    	String token = TokenUtil.createToken("1234567890", UMConstants.ANONYMOUS_USER_ID);
    	Context.destroyIdentityCard(token);
    	login(UMConstants.ANONYMOUS_USER_ID, Anonymous.one.getLoginName()); 
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
    
    protected Group createGroup(String name, Long parentId) {
		Group g = new Group();
        g.setParentId(parentId);
        g.setName(name);
        g.setGroupType( Group.MAIN_GROUP_TYPE );
        groupService.createNewGroup(g , "", "");
        return g;
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
    
	public static void excuteSQLFile(String sqlDir) {  
        log.info("正在执行目录：" + sqlDir+ "下的SQL脚本。。。。。。");  
        
        Pool connePool = JCache.getInstance().getConnectionPool();
		Cacheable connItem = connePool.checkOut(0);
		
        try {  
        	Connection conn = (Connection) connItem.getValue();
            Statement stmt = conn.createStatement();  
            
            List<File> sqlFiles = FileHelper.listFilesByTypeDeeply(".sql", new File(sqlDir));
            for(File sqlFile : sqlFiles) {
            	String fileName = sqlFile.getName();

            	log.info("开始执行SQL脚本：" + fileName+ "。");  
            	
                String sqls = FileHelper.readFile(sqlFile, "UTF-8");
                String[] sqlArray = sqls.split(";");
                for(String sql : sqlArray) {
                	if( EasyUtils.isNullOrEmpty(sql) ) continue;
                	
                	log.debug(sql);  
                	stmt.execute(sql);
                }
				
                log.info("SQL脚本：" + fileName+ " 执行完毕。");  
            }
 
            log.info("成功执行目录：" + sqlDir+ "下的SQL脚本!");
            stmt.close(); 
            
        } catch (Exception e) {  
            throw new RuntimeException("目录：" + sqlDir+ "下的SQL脚本执行出错：", e);
        } finally {
        	connePool.checkIn(connItem);
        }
    }
	
    public static void callAPI(String url, String user, String uToken) throws HttpException, IOException {
    	if(url.indexOf("?") < 0) {
    		url += "?uName=" +user+ "&uToken=" + uToken;
    	}
    	else {
    		url += "&uName=" +user+ "&uToken=" + uToken;
    	}
    	PostMethod postMethod = new PostMethod(url);
    	postMethod.getParams().setParameter(HttpMethodParams.HTTP_CONTENT_CHARSET, "utf-8");

        // 最后生成一个HttpClient对象，并发出postMethod请求
        HttpClient httpClient = new HttpClient();
        int statusCode = httpClient.executeMethod(postMethod);
        if(statusCode == 200) {
            System.out.print("返回结果: ");
            String soapResponseData = postMethod.getResponseBodyAsString();
            System.out.println(soapResponseData);     
        }
        else {
            System.out.println("调用失败！错误码：" + statusCode);
        }
    }
}
