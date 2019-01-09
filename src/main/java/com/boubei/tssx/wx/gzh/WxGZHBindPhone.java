package com.boubei.tssx.wx.gzh;

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
@Table(name = "wx_gzh_bind")
@SequenceGenerator(name = "wx_gzh_bind_sequence", sequenceName = "wx_gzh_bind_sequence", initialValue = 1, allocationSize = 10)
public class WxGZHBindPhone implements IEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "wx_gzh_bind_sequence")
	private Long id;

	@Column(nullable = false)
	private String mobile;

	@Column(nullable = false)
	private String openid;

	@Column(nullable = false)
	private String appid;

	private String unionid;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Serializable getPK() {
		return this.getId();
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getOpenid() {
		return openid;
	}

	public void setOpenid(String openid) {
		this.openid = openid;
	}

	public String getAppid() {
		return appid;
	}

	public void setAppid(String appid) {
		this.appid = appid;
	}

	public String getUnionid() {
		return unionid;
	}

	public void setUnionid(String unionid) {
		this.unionid = unionid;
	}

}
