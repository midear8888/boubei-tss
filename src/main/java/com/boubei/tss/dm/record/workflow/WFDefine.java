/* ==================================================================   
 * Created [2018-07-26] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.record.workflow;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.boubei.tss.framework.persistence.IEntity;

@Entity
@Table(name = "dm_workflow_def")
@SequenceGenerator(name = "workflow_def_sequence", sequenceName = "workflow_def_sequence", initialValue = 1, allocationSize = 10)
public class WFDefine implements IEntity {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "workflow_def_sequence")
	private Long id;
	
	@Column(nullable = false)
	private Long tableId;
	
	private String domain;
	
	@Column(length = 2000)
	private String define;

	public Serializable getPK() {
		return this.getId();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getTableId() {
		return tableId;
	}

	public void setTableId(Long tableId) {
		this.tableId = tableId;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getDefine() {
		return define;
	}

	public void setDefine(String define) {
		this.define = define;
	}
}
