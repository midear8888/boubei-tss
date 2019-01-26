package com.boubei.tss.modules.cloud;

import java.util.ArrayList;
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
@RequestMapping("/auth/module/account")
public class AccoutAction {

	@Autowired
	private ICommonService commonService;

	// 消费扣款

	// 查看余额
	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public Account queryAccount() {

		Long belong_user_id = Environment.getUserId();
		@SuppressWarnings("unchecked")
		List<Account> accounts = (List<Account>) commonService.getList(" from Account where belong_user_id = ?", belong_user_id);
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
		return commonService.getList(" from AccountFlow where account_id = ? order by id desc", account_id);
	}
	
	@RequestMapping(value = "/subauthorize", method = RequestMethod.GET)
	@ResponseBody
	public List<?> querySubAuthorize(){
		return commonService.getList(" from SubAuthorize where creatorId = ? order by id desc", Environment.getUserId());
	}

}
