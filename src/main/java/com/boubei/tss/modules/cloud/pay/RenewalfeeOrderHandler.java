package com.boubei.tss.modules.cloud.pay;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.time.DateUtils;

import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.cloud.entity.CloudOrder;
import com.boubei.tss.um.entity.SubAuthorize;

/**
 * @author hank 续费
 */
public class RenewalfeeOrderHandler extends ModuleOrderHandler {

	public RenewalfeeOrderHandler(CloudOrder co) {
		super(co);
	}
	
	/* 
	 * 续费不需要限制 购买账号, 只限制月份
	 */
	protected void beforeOrderModuleCheck() { 
		checkLimit(md.getMonth_limit(), co.getMonth_num(), "月", true, true); // 购买月份限制为1,1的不支持续费
	}

	protected void beforeOrderCustomCheck() {
		List<SubAuthorize> saList = querySaList();
		co.setAccount_num(saList.size());
		
		Set<Long> module_ids = new HashSet<>();
		for (SubAuthorize sa : saList) {
			module_ids.add(sa.getModuleId());
			
			if (!sa.getBuyerId().equals(Environment.getUserId())) {
				throw new BusinessException("您不能续费其它用户购买的账号！");
			}
		}

		if (module_ids.size() > 1) {
			throw new BusinessException("您不能同时续费多个产品，请分开续费！");
		}
	}
	
	@SuppressWarnings("unchecked")
	private List<SubAuthorize> querySaList() {
		String subAuthorizeIds = co.getParams();
		String hql = "from SubAuthorize where id in (" + subAuthorizeIds + ")";
		return (List<SubAuthorize>) dao.getEntities( hql );
	}

	protected void handle() {
		List<SubAuthorize> subAuthorizes = querySaList();
		Date now = new Date();
		for (SubAuthorize strategy : subAuthorizes) {
			Date endDate = new Date(Math.max(strategy.getEndDate().getTime(), now.getTime()));
			strategy.setEndDate(DateUtils.addMonths(endDate, co.getMonth_num()));
			strategy.setDisabled(0);
		}

		createFlows(getBuyerAccount());
	}

}
