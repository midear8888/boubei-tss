package com.boubei.tss.modules.cloud.product;

import com.boubei.tss.modules.cloud.entity.CloudOrder;

/**
 * @author hank 购买模块成功后续操作
 */
public class ModuleOrderEFFHandler extends AbstractAfterPay {

	public ModuleOrderEFFHandler(CloudOrder co) {
		super(co);
	}

	public Boolean handle() {
		apiService.mockLogin(userCode);
		//将账号移至业务员组下
		
		//将客户表内的客户停用
		
		//创建员工表内数据
		
		return true;
	}

}
