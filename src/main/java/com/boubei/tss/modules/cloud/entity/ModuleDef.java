/* ==================================================================   
 * Created [2016-06-22] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.cloud.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import com.boubei.tss.dm.record.ARecordTable;
import com.boubei.tss.util.EasyUtils;

@Entity
@Table(name = "cloud_module_def")
@SequenceGenerator(name = "module_def_seq", sequenceName = "module_def_seq", initialValue = 1, allocationSize = 10)
@JsonIgnoreProperties(value={"pk"})
public class ModuleDef extends ARecordTable {
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "module_def_seq")
    private Long id; 
    
	@Column(nullable = false)
	private String module;
	
	private String code;
	
	private String kind;
	
	@Column(nullable = false)
	private String roles;
	
	private String reports;
	
	private String records;   // 模块包含的数据表，为空则忽略
	
	private String init_url;  // 模块自定义初始化接口地址，在企业域用户选择此模块时自动调用，以完成模块初始化
	
	@Column(length = 500)
	private String resource; // 资源目录，多个用逗号分隔
	
	private String status; // creating|opened|closed
	
	private Integer seqno = 0; // 排列次序
	
	private Double price1;    // 单价（账号/月）
	private String price_def; // 优惠定义，eg: {mouth: 1,6,12,24,36|1,0.95,0.9,0.75,0.5, accout:1,7|300,0}
	
	private Integer try_days = 31; // 试用天数, 如果=0，表示不支持试用
	
	@Column(length = 800)
	private String description; //模块描述
	
	@Column(length = 800)
	private String remark;   // 模块备注
	
	public List<Long> roles() {
		List<Long> roleIds = new ArrayList<Long>();
		String[] array = EasyUtils.obj2String(roles).split(",");
		for(String t : array) {
			try {
				roleIds.add( EasyUtils.obj2Long(t) );
			} catch( Exception e) { }
		}
		
		roleIds.remove(0L);
		return roleIds;
	}
	
	public List<Long> reports() {
		List<Long> reportIds = new ArrayList<Long>();
		String[] array = EasyUtils.obj2String(reports).split(",");
		for(String t : array) {
			try {
				reportIds.add( EasyUtils.obj2Long(t) ); // 非数字的忽略
			} catch( Exception e) { }
		}
		
		reportIds.remove(0L);
		return reportIds;
	}
	
	public List<Long> records() {
		List<Long> recordIds = new ArrayList<Long>();
		String[] array = EasyUtils.obj2String(records).split(",");
		for(String t : array) {
			try {
				recordIds.add( EasyUtils.obj2Long(t) );
			} catch( Exception e) { }
		}
		
		recordIds.remove(0L);
		return recordIds;
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

	public String getModule() {
		return module;
	}

	public void setModule(String module) {
		this.module = module;
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public String getRoles() {
		return roles;
	}

	public void setRoles(String roles) {
		this.roles = roles;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public String getReports() {
		return reports;
	}

	public void setReports(String reports) {
		this.reports = reports;
	}

	public String getRecords() {
		return records;
	}

	public void setRecords(String records) {
		this.records = records;
	}

	public String getInit_url() {
		return init_url;
	}

	public void setInit_url(String init_url) {
		this.init_url = init_url;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Integer getSeqno() {
		return seqno;
	}

	public void setSeqno(Integer seqno) {
		this.seqno = seqno;
	}

	public Double getPrice1() {
		return price1;
	}

	public void setPrice1(Double price1) {
		this.price1 = price1;
	}

	public String getPrice_def() {
		return price_def;
	}

	public void setPrice_def(String price_def) {
		this.price_def = price_def;
	}

	public Integer getTry_days() {
		return try_days;
	}

	public void setTry_days(Integer try_days) {
		this.try_days = try_days;
	}

}
