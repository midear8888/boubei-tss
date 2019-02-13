package com.boubei.tss.modules.cloud.pay;

import java.util.List;

import com.boubei.tss.modules.cloud.entity.ModuleUser;

/**
 * @author hank 购买模块成功后续操作
 */
public class ModuleOrderHandler extends AbstractProduct {

	protected void handle() {

		// 注册企业域，并将用户移动到新建的域下作为域管理员
		// TODO 判断user是否已经是域管理员，是的话无需再注册
		userService.regBusiness(user, user.getUdf());

		// 创建模块授权策略
		Long module_id = co.getModule_id();
		createSubAuthorize();

		// 如果此时还没有选择 试用模块， 在此创建 ModuleUser 映射关系
		String domain = user.getDomain();

		String hql = "from ModuleUser where userId = ? and moduleId = ? and domain = ?";
		List<?> list = commonDao.getEntities(hql, userId, module_id, domain);
		if (list.isEmpty()) {
			ModuleUser mu = new ModuleUser(userId, module_id);
			mu.setDomain(domain);
			commonDao.create(mu);
		}

		createFlows( getAccount() );
	}
	
	public String getName() {
		return md.getModule();
	}

}
