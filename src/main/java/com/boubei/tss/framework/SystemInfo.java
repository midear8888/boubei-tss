/* ==================================================================   
 * Created [2009-7-7] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.modules.api.APIService;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.sso.online.DBOnlineUser;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MacrocodeCompiler;

@Controller
@RequestMapping("/si")
public class SystemInfo {
	
	@Autowired private ICommonService commonService;
	@Autowired private APIService apiService;
	
	@RequestMapping(value = "/version", method = RequestMethod.GET)
	@ResponseBody
	public Object[] getVersion(String sessionId) {
		String packageTime = Config.getAttribute("last.package.time");
		String environment = Config.getAttribute("environment");
		
		Date d = DateUtil.parse(packageTime);
		d = DateUtil.addDays(d, 1d/3);  //  +8小时，变成北京时间
		packageTime = DateUtil.format(d, "yyyy-MM-dd HH:mm:ss");
		
		return new Object[] { packageTime, environment, !Environment.isAnonymous() };
	}
	
	@RequestMapping(value = "/ui/{sessionAttr}", method = RequestMethod.GET)
	@ResponseBody
	public Object[] getLoginUser(@PathVariable String sessionAttr) {
		sessionAttr = (String) EasyUtils.checkNull(sessionAttr, DMConstants.USER_CODE);
		String result = DMUtil.fmParse( MacrocodeCompiler.createMacroCode(sessionAttr));
		return new Object[] { result };
	}
	
	/** 剔除在线用户 */
	@RequestMapping(value = "/su", method = RequestMethod.PUT)
	@ResponseBody
	public Object su(HttpServletResponse response, String target) {	
		if( !Environment.isAdmin() ) return null;
		
		String sessionId = Context.getRequestContext().getSessionId();
		List<?> list = commonService.getList("from DBOnlineUser where sessionId = ?", sessionId);
		for(Object o : list) {
			commonService.delete(DBOnlineUser.class, ((DBOnlineUser)o).getId() );
		}
		
		Context.sessionMap.get(sessionId).setAttribute("domain_multiLogin", ParamConstants.TRUE);
		String token = apiService.mockLogin(target);
		return token;
	}
	
	@RequestMapping(value = "/test", method = RequestMethod.GET)
	@ResponseBody
	public Object[] getThreadInfos() {
		ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);  
		Long random = null;
		try {
			random = SecureRandom.getInstance("SHA1PRNG").nextLong();
		} catch (NoSuchAlgorithmException e) {
		}
		return new Object[] { threadMXBean.getThreadCount(), threadInfos, random };
	}
}
