package com.boubei.tss.modules.cloud.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.boubei.tss.framework.persistence.IEntity;

/**
 * 域账户
 */
@Entity
@Table(name = "cloud_account")
@SequenceGenerator(name = "account_seq", sequenceName = "account_seq", initialValue = 1, allocationSize = 10)
public class Account implements IEntity {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "account_seq")
	private Long id;
	
	private Long belong_user_id;
	
	private String domain;
	
	private Double balance;        // 余额
	
	private Double balance_freeze; // 冻结余额
	
	private Double balance_safe;   // 安全额度
	
	private Date createDate;
	
	private String status;   // 激活|冻结
	
	private String remark;  // 备注

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getBelong_user_id() {
		return belong_user_id;
	}

	public void setBelong_user_id(Long belong_user_id) {
		this.belong_user_id = belong_user_id;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public Double getBalance() {
		return balance;
	}

	public void setBalance(Double balance) {
		this.balance = balance;
	}

	public Double getBalance_freeze() {
		return balance_freeze;
	}

	public void setBalance_freeze(Double balance_freeze) {
		this.balance_freeze = balance_freeze;
	}

	public Double getBalance_safe() {
		return balance_safe;
	}

	public void setBalance_safe(Double balance_safe) {
		this.balance_safe = balance_safe;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public Serializable getPK() {
		return this.getId();
	}
}