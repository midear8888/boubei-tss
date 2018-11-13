/* ==================================================================   
 * Created [2016-06-22] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.cloud;

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
	
//	private Double price1;
//	private Double price3;  // month * 12 * 0.9
//	private Double price6;  // month * 12 * 0.85
//	private Double price12; // month * 12 * 0.8
//	private Double price24; // month * 24 * 0.7
// 	private Double price36; // month * 36 * 0.5
	
	@Column(length = 2000)
	private String description;
	
	@Column(length = 2000)
	private String remark;
	
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
}
