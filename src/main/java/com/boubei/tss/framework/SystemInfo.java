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

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.util.DateUtil;

@Controller
@RequestMapping("/si")
public class SystemInfo {
	
	@RequestMapping(value = "/version", method = RequestMethod.GET)
	@ResponseBody
	public Object[] getVersion() {
		String packageTime = Config.getAttribute("last.package.time");
		String environment = Config.getAttribute("environment");
		
		Date d = DateUtil.parse(packageTime);
		d = DateUtil.addDays(d, 1d/3);  //  +8小时，变成北京时间
		packageTime = DateUtil.format(d, "yyyy-MM-dd HH:mm:ss");
		
		return new Object[] { packageTime, environment };
	}
	
	@RequestMapping(value = "/user", method = RequestMethod.GET)
	@ResponseBody
	public Object[] getLoginUser() {
		return new Object[] { Environment.getUserCode() };
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
