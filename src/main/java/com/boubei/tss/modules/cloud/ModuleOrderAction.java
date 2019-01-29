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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.cloud.entity.CloudOrder;
import com.boubei.tss.modules.cloud.product.AfterPayService;

@Controller
@RequestMapping("/auth/module/order")
public class ModuleOrderAction {

	@Autowired
	private ModuleService service;
	@Autowired
	private ICommonService commonService;

	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	public CloudOrder createOrder(CloudOrder mo) {
		return service.createOrder(mo);
	}

	@RequestMapping(method = RequestMethod.PUT)
	@ResponseBody
	public CloudOrder updateOrder(CloudOrder mo) {
		return service.updateOrder(mo);
	}

	@RequestMapping(value = "/price/query")
	@ResponseBody
	public Object queryPrice(CloudOrder mo) {
		return service.calMoney(mo, false);
	}

	@RequestMapping(value = "/price", method = RequestMethod.POST)
	@ResponseBody
	public Object updatePrice(Long id, Double rebate, Double derate) {
		CloudOrder mo = (CloudOrder) commonService.getEntity(CloudOrder.class, id);
		mo.setRebate(rebate);
		mo.setDerate(derate);
		mo.setMoney_real(mo.getMoney_cal() * rebate - derate);

		commonService.update(mo);
		return null;
	}

	@RequestMapping(value = "/list", method = RequestMethod.GET)
	@ResponseBody
	public List<?> listOrders() {
		String hql = "from CloudOrder where creator = ? order by id desc";
		return commonService.getList(hql, Environment.getUserCode());
	}

	@RequestMapping(value = "/payed/{order_no}", method = RequestMethod.POST)
	@ResponseBody
	public Object payedOrders(@PathVariable String order_no) {
		AfterPayService afterPayService = (AfterPayService) Global.getBean("ModuleService");
		CloudOrder co = (CloudOrder) commonService.getList(" from CloudOrder where order_no = ?", order_no).get(0);
		return afterPayService.handle(order_no, co.getMoney_cal(), "admin", "线下", null);

	}
}
