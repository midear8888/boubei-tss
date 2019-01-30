package com.boubei.tss.modules.cloud;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.framework.persistence.ICommonDao;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.cloud.entity.Account;

@Controller
@RequestMapping("/auth/account")
public class AccoutAction {

	@Autowired
	private ICommonDao commondao;
	@Autowired
	private ModuleService service;

	// 消费扣款

	// 查看余额
	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public Account queryAccount() {

		Long belong_user_id = Environment.getUserId();
		@SuppressWarnings("unchecked")
		List<Account> accounts = (List<Account>) commondao.getEntities(" from Account where belong_user_id = ?", belong_user_id);
		if (accounts.size() > 0) {
			return accounts.get(0);
		}
		Account account = new Account();
		account.setBelong_user_id(belong_user_id);
		account.setBalance(0D);
		return account;
	}

	// 提现

	// 查看账户流水
	@RequestMapping(value = "/flow", method = RequestMethod.GET)
	@ResponseBody
	public List<?> queryAccountFlow() {
		Long account_id = queryAccount().getId();
		if (account_id == null) {
			return new ArrayList<>();
		}
		return commondao.getEntities(" from AccountFlow where account_id = ? order by id desc", account_id);
	}

	@RequestMapping(value = "/subauthorize", method = RequestMethod.GET)
	@ResponseBody
	public List<?> querySubAuthorize() {
		return commondao.getEntities(" from SubAuthorize where creatorId = ? order by id desc", Environment.getUserId());
	}

	@RequestMapping(value = "/subauthorize/role", method = RequestMethod.GET)
	@ResponseBody
	public List<?> querySubAuthorizeRoles(Long strategyId) {
		return commondao.getEntities("select u, r.name from RoleUser u, Role r where u.strategyId = ? and u.roleId = r.id", strategyId);
	}

	@RequestMapping(value = "/subauthorize/role", method = RequestMethod.POST)
	@ResponseBody
	public Boolean setSubAuthorizeRoles(Long userId, String roleIds, Long strategyId) {
		service.setSubAuthorizeRoles(userId, roleIds, strategyId);
		return true;
	}

}
