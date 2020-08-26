/* ==================================================================   
 * Created [2009-7-7] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.persistence;

import com.boubei.tss.modules.log.Logable;

public interface ICommonDao extends IDao<IEntity> {
	
	@Logable(operateObject="${table}", operateInfo="create obj: ${args[0]} ")
	Object createWithLog(Object entity);
	
	@Logable(operateObject="${table}", operateInfo="update obj: ${args[0]} ")
	Object updateWithLog(Object entity);
	
	Object update(Object entity);
	
	@Logable(operateObject="${table}", operateInfo="删除了记录 ${returnVal} ")
	Object deleteWithLog(Class<?> entityClass, Long id);

}
