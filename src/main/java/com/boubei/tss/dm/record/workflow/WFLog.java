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

import com.boubei.tss.dm.record.ARecordTable;

/**
 * yyyy-MM-dd hh:mi:ss 谁执行了什么操作 备注信息 附件信息
 * 
 * 报表：《流程处理效率：平均每步处理时间》
 *  全年流程总体效率趋势图
	流程时间效率及时间效率同比、环比
	各岗位（角色）流程负荷情况
	流程超时情况统计
	岗位回退流程数据
	错误流程数量情况
	同岗位流程效率分析
	
核心操作：通过、驳回、会签、转办、中止、挂起，如何自动化测试工作流（所有路由组合）
会签：多个人一起签
批量操作：自行加按钮完成 batchOpt('批量审批', "status", "已通过", "r1,r2", "g1, g2", checkfn);

【待办事项】面板 如何计算一个人当前有多少个待处理流程

 */

@Entity
@Table(name = "wf_log")
@SequenceGenerator(name = "wf_log_sequence", sequenceName = "wf_log_sequence", initialValue = 1, allocationSize = 10)
public class WFLog extends ARecordTable {
	
	public static Long WF_LOG_TID = -101L;  // 流程处理时允许上传附件，使用此处虚拟负数ID作为标识
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "wf_log_sequence")
	private Long id;
	
	@Column(nullable = false)
	private Long tableId;  // 流程对应数据表
	
	@Column(nullable = false)
	private Long itemId;  // 对应的数据记录
	
	@Column(nullable = false)
	private String processer;
	
	@Column(nullable = false)
	private Date   processerTime1; // 开始处理时间，记录用户打开流程状态的时间
	
	@Column(nullable = false)
	private Date   processerTime2; // 结束处理时间
	
	@Column(length = 1000)
	private String processResult;  // 处理结果
	
	private String curstep;
	private String nextStep;
	private String nextStepProcesser;

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

	public String getProcesser() {
		return processer;
	}

	public void setProcesser(String processer) {
		this.processer = processer;
	}

	public Date getProcesserTime1() {
		return processerTime1;
	}

	public void setProcesserTime1(Date processerTime1) {
		this.processerTime1 = processerTime1;
	}

	public Date getProcesserTime2() {
		return processerTime2;
	}

	public void setProcesserTime2(Date processerTime2) {
		this.processerTime2 = processerTime2;
	}

	public String getProcessResult() {
		return processResult;
	}

	public void setProcessResult(String processResult) {
		this.processResult = processResult;
	}

	public String getCurstep() {
		return curstep;
	}

	public void setCurstep(String curstep) {
		this.curstep = curstep;
	}

	public String getNextStep() {
		return nextStep;
	}

	public void setNextStep(String nextStep) {
		this.nextStep = nextStep;
	}

	public String getNextStepProcesser() {
		return nextStepProcesser;
	}

	public void setNextStepProcesser(String nextStepProcesser) {
		this.nextStepProcesser = nextStepProcesser;
	}

}
