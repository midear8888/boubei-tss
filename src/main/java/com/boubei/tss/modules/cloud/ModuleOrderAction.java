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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.cloud.entity.ModuleDef;
import com.boubei.tss.modules.cloud.entity.ModuleOrder;
import com.boubei.tss.modules.cloud.entity.ModuleUser;
import com.boubei.tss.um.entity.SubAuthorize;

@Controller
@RequestMapping("/auth/module/order")
public class ModuleOrderAction {
	
	@Autowired private ModuleService service;
	@Autowired private ICommonService commonService;
	
	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	public Object createOrder(ModuleOrder mo) {

		calMoney(mo);
		
		mo.setOrder_date( new Date() );
		mo.setStatus(ModuleOrder.NEW);
		commonService.create(mo);
		return mo;
	}

	private void calMoney(ModuleOrder mo) {
		ModuleDef md = (ModuleDef) commonService.getEntity(ModuleDef.class, mo.getModule_id());
		mo.setPrice( md.getPrice1() );
		mo.setMoney_cal( md.getPrice1() * mo.getMonth_num() ); 
		// 价格以后台计算为准，防止篡改  （同时检查前后台的报价是否一致）  TODO 折扣优惠，创建一个计算价格的接口
	}
	
	@RequestMapping(method = RequestMethod.PUT)
	@ResponseBody
	public Object updateOrder(ModuleOrder mo) {
		// 重新计算价格
		calMoney(mo);
		
		commonService.update(mo);
		return mo;
	}
	
	@RequestMapping(value = "/price", method = RequestMethod.POST)
	@ResponseBody
	public Object updatePrice(Long id, Double rebate, Double derate) {
		ModuleOrder mo = (ModuleOrder) commonService.getEntity(ModuleOrder.class, id);
		mo.setRebate(rebate);
		mo.setDerate(derate);
		mo.setMoney_real( mo.getMoney_cal() * rebate - derate );
		
		commonService.update(mo);
		return null;
	}
	
	@RequestMapping(value = "/{id}", method = RequestMethod.POST)
	@ResponseBody
	public Object payOrder(@PathVariable Long id) {
		Long userId = Environment.getUserId();
		ModuleOrder mo = (ModuleOrder) commonService.getEntity(ModuleOrder.class, id);
		mo.setPay_date( new Date() );
		mo.setStatus(ModuleOrder.PAYED);
		
		int account_num = mo.getAcount_num();
		int mouth_num = mo.getMonth_num();
		for(int i = 0; i < account_num; i++) {
			SubAuthorize sa = new SubAuthorize();
			sa.setName( mo.getModule_id() + "_" + userId + "_" + i );
			sa.setStartDate(new Date());
			sa.setOwnerId( userId );
			
			Calendar calendar = new GregorianCalendar();
	        calendar.add(Calendar.MONTH, mouth_num);
			sa.setEndDate(calendar.getTime());
			
			commonService.create(sa);
		}
		
		// 如果此时还没有选择 试用模块， 在此创建 ModuleUser 映射关系
		String domain = Environment.getDomain();
		String hql = "from ModuleUser where userId = ? and moduleId = ? and domain = ?";
		List<?> list = commonService.getList(hql, Environment.getUserId(), mo.getModule_id(), domain);
		if( list.isEmpty() ) {
			ModuleUser mu = new ModuleUser(Environment.getUserId(), mo.getModule_id());
			mu.setDomain( domain );
			commonService.create(mu);
		}
		
		return "Success";
	}
	
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	@ResponseBody
	public List<?> listOrders() {
		String hql = "from ModuleOrder where creator = ?";
		return commonService.getList(hql, Environment.getUserCode());
	}
}
