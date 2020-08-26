package com.boubei.tss.modules.sn;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.boubei.tss.dm.record.ARecordTable;
import com.boubei.tss.modules.log.LogDisable;

/**
 * 自有单号，优先使用
 */
@LogDisable
@Entity
@Table(name = "x_selfno")
@SequenceGenerator(name = "selfno_sequence", sequenceName = "selfno_sequence", initialValue = 1, allocationSize = 10)
public class SelfNO extends ARecordTable {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "selfno_sequence")
	private Long id;
	
	@Column(nullable = false)
	private String code;
	
	@Column(nullable = false)
	private String tag;
	
	private Integer used;
	
	private String udf;
	

	public Serializable getPK() {
		return this.id;
	}

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

	public Integer getUsed() {
		return used;
	}

	public void setUsed(Integer used) {
		this.used = used;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getUdf() {
		return udf;
	}

	public void setUdf(String udf) {
		this.udf = udf;
	}
}
