package com.boubei.tss.modules.cloud.pay;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;

import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.cloud.entity.CloudOrder;
import com.boubei.tss.modules.cloud.entity.ModuleDef;
import com.boubei.tss.um.entity.SubAuthorize;
import com.boubei.tss.util.EasyUtils;

/**
 * @author hank 转授策略续费成功后续操作
 */
@SuppressWarnings("unchecked")
public class RenewalfeeOrderHandler extends AbstractProduct {
	
	public void beforeOrder(CloudOrder co) {
		String subAuthorizeIds = co.getParams();
		List<SubAuthorize> subAuthorizes = (List<SubAuthorize>) commonDao.getEntities(" from SubAuthorize where id in (" + subAuthorizeIds + ")");
		List<String> module_ids = new ArrayList<>();
		for (SubAuthorize subAuthorize : subAuthorizes) {
			String[] name = subAuthorize.getName().split("_");
			String module_id = name[0];
			if (!module_ids.contains(module_id)) {
				module_ids.add(module_id);
			}
			if (!subAuthorize.getBuyerId().equals( Environment.getUserId() )) {
				throw new BusinessException("您不能操作别的用户的可分配资源！");
			}
		}
		
		if (module_ids.size() > 1) {
			throw new BusinessException("您不能同时续费多个产品，请分开续费！");
		}
		
		Long module_id = EasyUtils.obj2Long( module_ids.get(0) );
		co.setModule_id(module_id);
		co.setAccount_num(subAuthorizes.size());
		commonDao.update(co);
		
		this.md = (ModuleDef) commonDao.getEntity(ModuleDef.class, module_id);
	}
 
	protected void handle() {
		// 获取订单信息
		String subAuthorizeIds = co.getParams();
		List<SubAuthorize> subAuthorizes = (List<SubAuthorize>) commonDao.getEntities(" from SubAuthorize where id in (" + subAuthorizeIds + ")");
		Date now = new Date();
		for (SubAuthorize subAuthorize : subAuthorizes) {
 
			if (subAuthorize.getEndDate().before(now)) {
				subAuthorize.setEndDate( now );
			}
			subAuthorize.setEndDate(DateUtils.addMonths(subAuthorize.getEndDate(), co.getMonth_num()));
			subAuthorize.setDisabled(0);
		}

		createFlows( getAccount() );
	}
	
	public String getName() {
		return PRODUCT_RENEWALFEE + " " + md.getModule();
	}

}
