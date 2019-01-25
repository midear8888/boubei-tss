package com.boubei.tss.modules.cloud.product;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import com.boubei.tss.modules.cloud.entity.CloudOrder;
import com.boubei.tss.modules.cloud.entity.ModuleDef;
import com.boubei.tss.modules.cloud.entity.ModuleUser;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.entity.SubAuthorize;
import com.boubei.tss.util.EasyUtils;

/**
 * @author hank
 * 购买模块成功后续操作
 */
public class ModuleOrderHandler extends AbstractAfterPay {

	public ModuleOrderHandler(CloudOrder co) {
		super(co);
	}

	public Boolean handle() {

		int account_num = co.getAccount_num();
		int mouth_num = co.getMonth_num();
		Long module_id = co.getModule_id();
		for (int i = 0; i < account_num; i++) {
			SubAuthorize sa = new SubAuthorize();
			
			ModuleDef def = (ModuleDef) commonDao.getEntity(ModuleDef.class, module_id);
			sa.setName(def.getId() + "_" + def.getModule() + "_" + i); // name: 模块ID_模块名称_购买序号
			
			sa.setStartDate(new Date());
			sa.setOwnerId(userId);

			Calendar calendar = new GregorianCalendar();
			calendar.add(Calendar.MONTH, mouth_num);
			sa.setEndDate(calendar.getTime());

			commonDao.create(sa);
		}

		// 如果此时还没有选择 试用模块， 在此创建 ModuleUser 映射关系
		String domain = (String) EasyUtils.checkNull(co.getDomain(), UMConstants.DEFAULT_DOMAIN);

		String hql = "from ModuleUser where userId = ? and moduleId = ? and domain = ?";
		List<?> list = commonDao.getEntities(hql, userId, module_id, domain);
		if (list.isEmpty()) {
			ModuleUser mu = new ModuleUser(userId, module_id);
			mu.setDomain(domain);
			commonDao.create(mu);
		}

		return true;

	}

}
