package com.boubei.tss.modules.cloud.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.boubei.tss.dm.record.ARecordTable;
import com.boubei.tss.modules.param.ParamConstants;

@Entity
@Table(name = "cloud_proxy_contract")
@SequenceGenerator(name = "proxy_contract_seq", sequenceName = "proxy_contract_seq", initialValue = 1, allocationSize = 10)
public class ProxyContract extends ARecordTable {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "proxy_contract_seq")
	private Long id;

	@Column(nullable = false)
	private String code;  // 合同号
	
	@ManyToOne
	private ModuleDef module; // 代理产品（模块）

	@Column(nullable = false)
	private Double price; // 代理价

	private String proxy_user;

	private String appid;
	
	private Double deposit; // 保证金
	
	private Date startDate; // 开始时间 
	private Date endDate;   // 结束时间 

	/** 状态：停用、启用 */
	@Column(nullable = false)
	private Integer disabled = ParamConstants.FALSE;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Double getPrice() {
		return price;
	}

	public void setPrice(Double price) {
		this.price = price;
	}

	public Integer getDisabled() {
		return disabled;
	}

	public void setDisabled(Integer disabled) {
		this.disabled = disabled;
	}

	public ModuleDef getModule() {
		return module;
	}

	public void setModule(ModuleDef module) {
		this.module = module;
	}

	public String getAppid() {
		return appid;
	}

	public void setAppid(String appid) {
		this.appid = appid;
	}

	public Serializable getPK() {
		return this.getId();
	}

	public String getProxy_user() {
		return proxy_user;
	}

	public void setProxy_user(String proxy_user) {
		this.proxy_user = proxy_user;
	}

	public Double getDeposit() {
		return deposit;
	}

	public void setDeposit(Double deposit) {
		this.deposit = deposit;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
}
