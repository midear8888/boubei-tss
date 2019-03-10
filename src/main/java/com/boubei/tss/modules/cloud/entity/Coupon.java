//package com.boubei.tss.modules.cloud.entity;
//
//import java.io.Serializable;
//import java.util.Date;
//
//import javax.persistence.Entity;
//import javax.persistence.GeneratedValue;
//import javax.persistence.GenerationType;
//import javax.persistence.Id;
//import javax.persistence.ManyToOne;
//import javax.persistence.SequenceGenerator;
//import javax.persistence.Table;
//
//import com.boubei.tss.framework.persistence.IEntity;
//import com.boubei.tss.um.entity.User;
//
///**
// * 优惠券
// */
//@Entity
//@Table(name = "cloud_coupon")
//@SequenceGenerator(name = "coupon_seq", sequenceName = "coupon_seq", initialValue = 1, allocationSize = 10)
//public class Coupon implements IEntity {
//
//	public static final String STATUS0 = "未使用";
//	public static final String STATUS1 = "已使用";
//
//	@Id
//	@GeneratedValue(strategy = GenerationType.AUTO, generator = "coupon_seq")
//	private Long id;
//
//	private String code;
//
//	private String name;
//
//	@ManyToOne
//	private User belong_user;
//
//	// 有效期
//	private Date validity_begin = new Date();
//	private Date validity_to;
//
//	// 购买模块限制
//	private Long module_limit;
//	// 实付金额限制 不低于这个值
//	private Double money_limit;
//
//	private String status = STATUS0;
//
//	private User used_user;
//	private Date used_time;
//
//	public Serializable getPK() {
//		return this.id;
//	}
//
//	public Long getId() {
//		return id;
//	}
//
//	public void setId(Long id) {
//		this.id = id;
//	}
//
//	public String getCode() {
//		return code;
//	}
//
//	public void setCode(String code) {
//		this.code = code;
//	}
//
//	public User getBelong_user() {
//		return belong_user;
//	}
//
//	public void setBelong_user(User belong_user) {
//		this.belong_user = belong_user;
//	}
//
//	public Date getValidity_begin() {
//		return validity_begin;
//	}
//
//	public void setValidity_begin(Date validity_begin) {
//		this.validity_begin = validity_begin;
//	}
//
//	public Date getValidity_to() {
//		return validity_to;
//	}
//
//	public void setValidity_to(Date validity_to) {
//		this.validity_to = validity_to;
//	}
//
//	public Long getModule_limit() {
//		return module_limit;
//	}
//
//	public void setModule_limit(Long module_limit) {
//		this.module_limit = module_limit;
//	}
//
//	public Double getMoney_limit() {
//		return money_limit;
//	}
//
//	public void setMoney_limit(Double money_limit) {
//		this.money_limit = money_limit;
//	}
//
//	public String getStatus() {
//		return status;
//	}
//
//	public void setStatus(String status) {
//		this.status = status;
//	}
//
//	public User getUsed_user() {
//		return used_user;
//	}
//
//	public void setUsed_user(User used_user) {
//		this.used_user = used_user;
//	}
//
//	public Date getUsed_time() {
//		return used_time;
//	}
//
//	public void setUsed_time(Date used_time) {
//		this.used_time = used_time;
//	}
//
//	public String getName() {
//		return name;
//	}
//
//	public void setName(String name) {
//		this.name = name;
//	}
//
//}
