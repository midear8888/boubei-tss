package com.boubei.tssx.wx;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.boubei.tss.dm.record.ARecordTable;

/**
 * 需要允许匿名写入:
 * 
 * 1、录入表【wx_log】的浏览权限授予匿名角色
 * 2、/xdata/wx_log 加入地址白名单， 相应html页面地址也加入地址白名单
 * 3、/xdata/wx_log?anonymous=true
 * 注：开放匿名访问了则无法在限制数据的域
 * 
 * http://localhost:9000/tss/ftl/tabledef?className=com.boubei.tssx.wx.WxLog
 */
@Entity
@Table(name = "wx_log")
@SequenceGenerator(name = "wx_log_sequence", sequenceName = "wx_log_sequence", initialValue = 1, allocationSize = 10)
public class WxLog extends ARecordTable {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "wx_log_sequence")
	private Long id;

	private String logType;  // debug | info | error
	private Date   date;
	private String msg; 
	private String brand;
	private String model;
	private String wx_version;
	private String system;
	private String platform; 
	private String sdk_version;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getLogType() {
		return logType;
	}
	public void setLogType(String logType) {
		this.logType = logType;
	}
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
	public String getBrand() {
		return brand;
	}
	public void setBrand(String brand) {
		this.brand = brand;
	}
	public String getModel() {
		return model;
	}
	public void setModel(String model) {
		this.model = model;
	}
	public String getWx_version() {
		return wx_version;
	}
	public void setWx_version(String wx_version) {
		this.wx_version = wx_version;
	}
	public String getSystem() {
		return system;
	}
	public void setSystem(String system) {
		this.system = system;
	}
	public String getPlatform() {
		return platform;
	}
	public void setPlatform(String platform) {
		this.platform = platform;
	}
	public String getSdk_version() {
		return sdk_version;
	}
	public void setSdk_version(String sdk_version) {
		this.sdk_version = sdk_version;
	}
	
	public Serializable getPK() {
		return this.getId();
	}
}
