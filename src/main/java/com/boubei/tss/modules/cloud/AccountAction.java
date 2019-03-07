package com.boubei.tss.modules.cloud;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.cloud.entity.Account;

@Controller
@RequestMapping({"/auth/account","/api/account"})
public class AccountAction {

	@Autowired private ICommonService commService;
	@Autowired private CloudService service;

	// 查看余额
	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public Account queryAccount() {
		Long userID = Environment.getUserId();
		List<?> accounts = commService.getList(" from Account where belong_user_id = ?", userID);
		if (accounts.size() > 0) {
			return (Account) accounts.get(0);
		}
		
		Account account = new Account();
		account.setBalance(0D);
		account.setId(-999L);
		return account;
	}

	// 查看账户流水
	@RequestMapping(value = "/flow", method = RequestMethod.GET)
	@ResponseBody
	public List<?> queryAccountFlow() {
		Long account_id = queryAccount().getId();
		return commService.getList(" from AccountFlow where account_id = ? order by id desc", account_id);
	}

	@RequestMapping(value = "/subauthorize", method = RequestMethod.GET)
	@ResponseBody
	public List<?> querySubAuthorize() {
		return commService.getList(" from SubAuthorize where buyerId = ? order by id desc", Environment.getUserId());
	}

	@RequestMapping(value = "/subauthorize/role", method = RequestMethod.GET)
	@ResponseBody
	public List<?> querySubAuthorizeRoles(Long strategyId) {
		String hql = "select ru, r.name from RoleUser ru, Role r " +
				" where ru.strategyId = ? and ru.roleId = r.id and ru.moduleId is not null order by r.decode desc";
		return commService.getList(hql, strategyId);
	}

	@RequestMapping(value = "/subauthorize/role", method = RequestMethod.POST)
	@ResponseBody
	public Boolean setSubAuthorizeRoles(Long userId, String ruIds, Long strategyId) {
		service.setSubAuthorizeRoles(userId, ruIds, strategyId);
		return true;
	}

}
