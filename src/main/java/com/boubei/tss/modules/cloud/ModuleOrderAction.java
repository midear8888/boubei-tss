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

import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.cloud.entity.CloudOrder;
import com.boubei.tss.modules.cloud.pay.AfterPayService;

@Controller
@RequestMapping("/cloud")
public class ModuleOrderAction {

	@Autowired private CloudService cloudService;
	@Autowired private ICommonService commonService;

	@RequestMapping(value = "/order", method = RequestMethod.POST)
	@ResponseBody
	public CloudOrder createOrder(CloudOrder mo) {
		return cloudService.createOrder(mo);
	}

	@RequestMapping(value = "/order", method = RequestMethod.PUT)
	@ResponseBody
	public CloudOrder updateOrder(CloudOrder mo) {
		cloudService.calMoney(mo); // 重新计算价格
		commonService.update(mo);
		
		return mo;
	}

	@RequestMapping(value = "/order/price/query")
	@ResponseBody
	public Object queryPrice(CloudOrder mo) {
		return cloudService.calMoney(mo);
	}

	@RequestMapping(value = "/order/price", method = RequestMethod.POST)
	@ResponseBody
	public void updatePrice(Long id, Double rebate, Double derate) {
		// 只有超级管理员有权限，才有权限对订单价格进行减免或给与折扣
		if( !Environment.isAdmin() ) return;
		
		CloudOrder mo = (CloudOrder) commonService.getEntity(CloudOrder.class, id);
		mo.setRebate(rebate);
		mo.setDerate(derate);
		mo.setMoney_real(mo.getMoney_cal() * rebate - derate);

		commonService.update(mo);
	}

	@RequestMapping(value = "/order/list", method = RequestMethod.GET)
	@ResponseBody
	public List<?> listOrders() {
		String hql = "from CloudOrder where creator = ? order by id desc";
		return commonService.getList(hql, Environment.getUserCode());
	}

	@RequestMapping(value = "/modules", method = RequestMethod.GET)
	@ResponseBody
	public List<?> listAvaliableModules() {
		return cloudService.listAvaliableModules();
	}
	
	/**
	 * Admin 设置订单状态为已支付
	 */
	@RequestMapping(value = "/order/payed/{order_no}", method = RequestMethod.POST)
	@ResponseBody
	public void payedOrders(@PathVariable String order_no) {
		AfterPayService afterPayService = (AfterPayService) cloudService;
		List<?> list = commonService.getList(" from CloudOrder where order_no = ?", order_no);
		CloudOrder co = (CloudOrder) list.get(0);
		
		afterPayService.handle(order_no, co.getMoney_cal(), "admin", "线下", null);
	}
}
