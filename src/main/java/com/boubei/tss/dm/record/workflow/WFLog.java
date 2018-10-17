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
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.boubei.tss.framework.persistence.IEntity;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.util.EasyUtils;

/**
 * 
 * 核心操作：通过、驳回、会签(多个人一起签)、转办
 * 
 * 报表：
 *     流程处理效率：平均每步处理时间
 *     全年流程总体效率趋势图
 * 	   流程时间效率及时间效率同比、环比
 */
@Entity
@Table(name = "dm_workflow_log")
@SequenceGenerator(name = "workflow_log_sequence", sequenceName = "workflow_log_sequence", initialValue = 1, allocationSize = 10)
public class WFLog implements IEntity {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "workflow_log_sequence")
	private Long id;
	
	@Column(nullable = false)
	private Long tableId;  // 流程对应数据表
	
	@Column(nullable = false)
	private Long itemId;  // 对应的数据记录
	
	/* 流程处理人、处理时间、处理结果、处理意见 */
	@Column(nullable = false)
	private String processor;
	
	@Column(nullable = false)
	private Date   processTime;
	
	private String processResult;
	
	private String processOpinion;
	
	private String origin; // 来源：微信、H5、PC
	
	public WFLog() { }
	
	public WFLog(WFStatus wfStatus, String opinion) {
		this.setTableId(wfStatus.getTableId());
		this.setItemId(wfStatus.getItemId());
		this.setProcessor( Environment.getUserName() );
		this.setProcessTime( new Date() );
		this.setProcessResult( wfStatus.getCurrentStatus() );
		this.setProcessOpinion( EasyUtils.obj2String(opinion) );
		this.setOrigin( Environment.getOrigin() );
	}
	
	public String toString() {
		return WFUtil.toString(this);
	}
	
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

	public Long getItemId() {
		return itemId;
	}

	public void setItemId(Long itemId) {
		this.itemId = itemId;
	}

	public String getProcessor() {
		return processor;
	}

	public void setProcessor(String processor) {
		this.processor = processor;
	}

	public String getProcessResult() {
		return processResult;
	}

	public void setProcessResult(String processResult) {
		this.processResult = processResult;
	}

	public Date getProcessTime() {
		return processTime;
	}

	public void setProcessTime(Date processTime) {
		this.processTime = processTime;
	}

	public String getProcessOpinion() {
		return processOpinion;
	}

	public void setProcessOpinion(String processOpinion) {
		this.processOpinion = processOpinion;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}
}
