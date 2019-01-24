/* ==================================================================   
 * Created [2016-06-22] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.cloud;

import java.util.List;
import java.util.Map;

import com.boubei.tss.modules.cloud.entity.CloudOrder;

public interface ModuleService {

	CloudOrder createOrder(CloudOrder mo);

	CloudOrder updateOrder(CloudOrder mo);

	CloudOrder calMoney(CloudOrder mo, Boolean throw_);

	List<?> listAvaliableModules();

	List<?> listSelectedModules(Long user);

	void unSelectModule(Long user, Long module);

	void selectModule(Long user, Long module);

	void refreshModuleUserRoles(Long module);

	Object payOrder(Map<?, ?> trade_map);

}
