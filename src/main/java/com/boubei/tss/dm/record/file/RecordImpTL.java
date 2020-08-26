/* ==================================================================   
 * Created [2019-9-20] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.record.file;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.boubei.tss.dm.record.ARecordTable;
import com.boubei.tss.modules.param.ParamConstants;

/**
 * 数据表Excel导入自定义模板 (域级、客户级)
 * 
[
  {'label':'模板类别','code':'type','nullable':'false','isparam':'true'},
  {'label':'模板名称','code':'tlname','nullable':'false','isparam':'true','unique':'true'},
  {'label':'模板定义','code':'tldef','height':'80','width':'320'},
  {'label':'外部表头','code':'origin_header','height':'60','width':'320'},
  {'label':'所属客户','code':'customer'},
  {'label':'模板状态','code':'status','type':'int','options':{'codes':'1|0','names':'是|否'}}
]
 */
@Entity
@Table(name = "dm_record_imptl")
@SequenceGenerator(name = "record_imptl_seq")
public class RecordImpTL extends ARecordTable {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "record_imptl_seq")
	private Long id;
	
	private String type;  // 模板类别，订单、ASN、SKU等
	private String tldef; 
	private String tlname;   // 模板名称
	private String origin_header;
	
	private String customer; // 所属客户（货主），以此字段作为导入条件
	private Integer status = ParamConstants.TRUE;  // 停用、启用

	public Serializable getPK() {
		return this.id;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTldef() {
		return tldef;
	}

	public void setTldef(String tldef) {
		this.tldef = tldef;
	}

	public String getTlname() {
		return tlname;
	}

	public void setTlname(String tlname) {
		this.tlname = tlname;
	}

	public String getCustomer() {
		return customer;
	}

	public void setCustomer(String customer) {
		this.customer = customer;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public String getOrigin_header() {
		return origin_header;
	}

	public void setOrigin_header(String origin_header) {
		this.origin_header = origin_header;
	}
}