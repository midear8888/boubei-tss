package com.boubei.tss.modules.cloud.product;

import java.util.List;

import com.boubei.tss.modules.cloud.entity.Account;
import com.boubei.tss.modules.cloud.entity.CloudOrder;
import com.boubei.tss.modules.cloud.entity.ModuleUser;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.util.EasyUtils;

/**
 * @author hank 购买模块成功后续操作
 */
public class ModuleOrderHandler extends AbstractAfterPay {

	public ModuleOrderHandler(CloudOrder co) {
		super(co);
	}

	public Boolean handle() {

		userService.regBusiness(user, user.getUdf());

		apiService.mockLogin(userCode);

		Long module_id = co.getModule_id();
		createSubAuthorize();

		// 如果此时还没有选择 试用模块， 在此创建 ModuleUser 映射关系
		String domain = (String) EasyUtils.checkNull(co.getDomain(), UMConstants.DEFAULT_DOMAIN);

		String hql = "from ModuleUser where userId = ? and moduleId = ? and domain = ?";
		List<?> list = commonDao.getEntities(hql, userId, module_id, domain);
		if (list.isEmpty()) {
			ModuleUser mu = new ModuleUser(userId, module_id);
			mu.setDomain(domain);
			commonDao.create(mu);
		}

		Account account = getAccount();
		createFlows(account);
		return true;
	}

}
