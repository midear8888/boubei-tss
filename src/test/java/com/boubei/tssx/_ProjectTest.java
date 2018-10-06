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

import java.util.Set;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.AbstractTest4;

public class _ProjectTest extends AbstractTest4 {

	@Autowired _Project _project;
	
	@Test
	public void test() {
		
		String pack = "com/boubei/tssx";
		
		Set<?> result = _project.getCodeFiles(pack, null);
		log.info( result );
		
		result = _project.getCodeFiles(pack, "java");
		log.info( result );
		
	}
}
