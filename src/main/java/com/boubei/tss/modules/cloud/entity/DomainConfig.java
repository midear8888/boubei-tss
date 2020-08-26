package com.boubei.tss.modules.cloud.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.boubei.tss.cache.aop.CacheClear;
import com.boubei.tss.dm.record.ARecordTable;
import com.boubei.tss.util.EasyUtils;

@Entity
@Table(name = "x_domain_config")
@SequenceGenerator(name = "x_domain_config_seq", sequenceName = "x_domain_config_seq", initialValue = 1, allocationSize = 10)
@CacheClear()
public class DomainConfig extends ARecordTable {
	
	public DomainConfig() {
	}

	public DomainConfig(String code, String content) {
		this.code = code;
		this.content = content;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "x_domain_config_seq")
	private Long id;

	public Serializable getPK() {
		return this.id;
	}

	@Column(nullable = false)
	private String code;
	@Lob
	private String content;

	private String user;
	private String remark;

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

	public String getContent() {
		return EasyUtils.isNullOrEmpty(content) ? null : content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public boolean _isTrue() {
		return "1".equals(content) || "true".equals(content);
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}
}
