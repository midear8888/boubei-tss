package com.boubei.tss;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.log4j.Logger;

import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.User;

public abstract class AbstractTest4 extends AbstractTest4TSS { 
 
    protected static Logger log = Logger.getLogger(AbstractTest4.class);    
    
    // 必须要先有个域登录
 	String domain = "BD";
 	User testUser;
 
    protected void init() {
    	super.init();
 
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
