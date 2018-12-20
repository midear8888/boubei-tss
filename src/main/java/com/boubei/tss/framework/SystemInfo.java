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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MacrocodeCompiler;

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
	
	@RequestMapping(value = "/ui/{sessionAttr}", method = RequestMethod.GET)
	@ResponseBody
	public Object[] getLoginUser(@PathVariable String sessionAttr) {
		sessionAttr = (String) EasyUtils.checkNull(sessionAttr, DMConstants.USER_CODE);
		String result = DMUtil.fmParse( MacrocodeCompiler.createMacroCode(sessionAttr));
		return new Object[] { result };
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
