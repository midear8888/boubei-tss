package com.boubei.tss.modules.cloud.product;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;

import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.modules.cloud.entity.Account;
import com.boubei.tss.modules.cloud.entity.CloudOrder;
import com.boubei.tss.um.entity.SubAuthorize;

/**
 * @author hank 转授策略续费成功后续操作
 */
public class RenewalfeeOrderHandler extends AbstractAfterPay {

	public RenewalfeeOrderHandler(CloudOrder co) {
		super(co);
	}

	@SuppressWarnings("unchecked")
	public Boolean handle() {
		// 获取订单信息
		String subAuthorizeIds = co.getParams();
		List<SubAuthorize> subAuthorizes = (List<SubAuthorize>) commonDao.getEntities(" from SubAuthorize where id in (" + subAuthorizeIds + ")");
		List<String> module_ids = new ArrayList<>();
		Date date = new Date();
		for (SubAuthorize subAuthorize : subAuthorizes) {
			String[] name = subAuthorize.getName().split("_");
			String module_id = name[0];
			if (!module_ids.contains(module_id)) {
				module_ids.add(module_id);
			}
			if (!subAuthorize.getCreatorId().equals(userId)) {
				throw new BusinessException("您不能操作别的用户的设备！");
			}
			if (subAuthorize.getEndDate().before(date)) {
				subAuthorize.setEndDate(date);
			}
			subAuthorize.setEndDate(DateUtils.addMonths(subAuthorize.getEndDate(), co.getMonth_num()));
		}
		if (module_ids.size() > 1) {
			throw new BusinessException("您不能同时续费多个产品，请分开续费！");
		}
		// ----校验结束----

		Account account = getAccount();
		createFlows(account);

		return true;
	}

}
