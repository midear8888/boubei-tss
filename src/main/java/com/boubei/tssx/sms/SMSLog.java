package com.boubei.tssx.sms;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.boubei.tss.dm.record.ARecordTable;

@Entity
@Table(name = "sms_log")
@SequenceGenerator(name = "sms_log_sequence", sequenceName = "sms_log_sequence", initialValue = 1, allocationSize = 10)
public class SMSLog extends ARecordTable {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sms_log_sequence")
	private Long id;
	
	@Column(nullable = false)
	private String phonenum;  // 接收人手机号
	
	@Column(nullable = false)
	private Integer randomnum;  // 随机数
	
	private String msg;
	
	private String sendDay;  // 日期，用以统计
	
	private String tlcode;
	private String params;


	public Serializable getPK() {
		return this.getId();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getPhonenum() {
		return phonenum;
	}

	public void setPhonenum(String phonenum) {
		this.phonenum = phonenum;
	}

	public Integer getRandomnum() {
		return randomnum;
	}

	public void setRandomnum(Integer randomnum) {
		this.randomnum = randomnum;
	}

	public String getSendDay() {
		return sendDay;
	}

	public void setSendDay(String sendDay) {
		this.sendDay = sendDay;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public String getTlcode() {
		return tlcode;
	}

	public void setTlcode(String tlcode) {
		this.tlcode = tlcode;
	}

	public String getParams() {
		return params;
	}

	public void setParams(String params) {
		this.params = params;
	}
}
