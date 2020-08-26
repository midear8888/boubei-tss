package com.boubei.tss.modules.cloud.pay;

import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.modules.cloud.entity.Account;
import com.boubei.tss.modules.cloud.entity.AccountFlow;
import com.boubei.tss.modules.cloud.entity.CloudOrder;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.util.EasyUtils;

/**
 * @author hank 充值成功后续操作
 */
public class RechargeOrderHandler extends AbstractProduct {

    public RechargeOrderHandler(CloudOrder co) {
        super(co);
    }

    public String getName() {
        return AccountFlow.TYPE1;
    }

    protected void handle() {
        Account account = getAccount(getRechargeAccountUser().getId());
        // createIncomeFlow(account);
        AccountFlow flow = new AccountFlow(account, this, AccountFlow.TYPE1, null);
        Double deltaMoney = (Double) EasyUtils.checkTrue(ADMIN_PAYER.equals(payer), co.getMoney_cal(), co.getMoney_real());
        createFlow(account, flow, deltaMoney);

        dao.update(account);
    }

    protected Double getProxyPrice() {
        return noProxy ? 0d : -co.getMoney_cal();
    }

    public void setPrice() {

    }

	public static final String ERROR1 = "充值账号在系统中不存在";

    protected void beforeOrderModuleCheck() {
        if (getRechargeAccountUser() == null) {

			throw new BusinessException(ERROR1);
        }
    }

    private User getRechargeAccountUser() {
        String targetUser = (String) this.params.get("targetUserCode");
        return  EasyUtils.isNullOrEmpty(targetUser) ? buyer : dao.getUserByAccount(targetUser, false);
    }
}
