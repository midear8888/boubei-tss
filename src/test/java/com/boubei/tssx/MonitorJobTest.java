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

import org.junit.Assert;
import org.junit.Test;

import com.boubei.tss.AbstractTest4;


public class MonitorJobTest extends AbstractTest4 {

	@Test
	public void testJob() {
		
		try {
			MonitorJob job = new MonitorJob();
        	job.excuteJob("10,www.boubei.com,卜贝");
		} 
		catch(Exception e) {
			Assert.assertFalse(true);
        }
	}
}
