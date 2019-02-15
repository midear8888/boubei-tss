package com.boubei.tss.modules.cloud.pay;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
		String hql = " from SubAuthorize where id in (" + subAuthorizeIds + ")";
		List<SubAuthorize> subAuthorizes = (List<SubAuthorize>) commonDao.getEntities(hql);
		Set<String> module_ids = new HashSet<>();
		for (SubAuthorize subAuthorize : subAuthorizes) {
			String[] name = subAuthorize.getName().split("_");
			String module_id = name[0];
			module_ids.add(module_id);
			if (!subAuthorize.getBuyerId().equals( Environment.getUserId() )) {
				throw new BusinessException("您不能续费其它用户购买的账号！");
			}
		}
		
		if (module_ids.size() > 1) {
			throw new BusinessException("您不能同时续费多个产品，请分开续费！");
		}
		
		Long module_id = EasyUtils.obj2Long( module_ids.toArray()[0] );
		co.setModule_id(module_id);
		co.setAccount_num(subAuthorizes.size());
		commonDao.update(co);
		
		this.md = (ModuleDef) commonDao.getEntity(ModuleDef.class, module_id);
	}
 
	protected void handle() {
		// 获取订单信息
		String hql = " from SubAuthorize where id in (" + co.getParams() + ")";
		List<SubAuthorize> subAuthorizes = (List<SubAuthorize>) commonDao.getEntities(hql);
		Date now = new Date();
		for (SubAuthorize strategy : subAuthorizes) {
			Date endDate = new Date( Math.max(strategy.getEndDate().getTime(), now.getTime()) );
			strategy.setEndDate(DateUtils.addMonths(endDate, co.getMonth_num()));
			strategy.setDisabled(0);
		}

		createFlows( getAccount() );
	}
	
	public String getName() {
		return PRODUCT_RENEWALFEE + " " + md.getModule();
	}

}
