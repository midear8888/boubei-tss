/* ==================================================================   
 * Created [2018-5-8] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.dml;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.boubei.tss.dm.record.ARecordTable;

/**
 * <p>
 * SQL自定义表
 * http://localhost:9000/tss/ftl/tabledef?className=com.boubei.tss.dm.dml.SQLDef
 * 
 * 
[
  {'label':'code','code':'code','nullable':'false','unique':'true','width':'180px'},
  {'label':'script','code':'script','nullable':'false','width':'300px','height':'80px','cwidth':'500px'},
  {'label':'remark','code':'remark','width':'300px','height':'80px','cwidth':'200px'}
]
 * </p>
 */
@Entity
@Table(name = "dm_sql_def")
@SequenceGenerator(name = "sql_def_seq")
public class SQLDef extends ARecordTable {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "sql_def_seq")
	private Long id;
	
	@Column(length = 100, nullable = false, unique=true)
	private String code;
	
	@Column(length = 1000, nullable = false)
	private String script;
	
	@Column(length = 1000)
	private String remark;

	public Serializable getPK() {
		return this.getId();
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

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}
	
}