package com.boubei.tss.modules.cloud;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.boubei.tss.dm.record.ARecordTable;

/**
 * 域信息扩展：必须由域账号自行创建，Admin等域外部创建的账号无效
 * 
 * http://localhost:9000/tss/xdata/json/x_domain?fields=*&name=卜数科技
 * http://localhost:9000/tss/ftl/tabledef?className=com.boubei.tss.modules.cloud.DomainInfo
 * 
[
  {'label':'域名','code':'name','nullable':'false'},
  {'label':'logo','code':'logo','type':'file'},
  {'label':'微信二维码','code':'wxpay_qr','type':'file'},
  {'label':'微信账号','code':'wxpay_account'},
  {'label':'支付宝二维码','code':'alipay_qr','type':'file'},
  {'label':'支付宝账号','code':'alipay_account'},
  {'label':'订单前缀','code':'prefix'},
  {'label':'短信签名','code':'sms_sign'},
  {'label':'短信账号','code':'sms_key'},
  {'label':'短信秘钥','code':'sms_secret'},
  {'label':'所处位置','code':'location'},
  {'label':'广告位图片','type':'file','code':'ggpic'},
  {'label':'联系方式','code':'contact_info','width':'350','height':'90'}
]
 */
@Entity
@Table(name = "x_domain")
@SequenceGenerator(name = "x_domain_seq")
public class DomainInfo extends ARecordTable {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "x_domain_seq")
	private Long id;
 
	private String name; // 域名
	
	private String logo;
	
	private String wxpay_qr;
	private String wxpay_account;
	
	private String alipay_qr;
	private String alipay_account;
	
	private String prefix;    // 订单前缀
	private String sms_sign;  // 短信签名
	private String sms_key;  
	private String sms_secret;
	
	private String location; // 企业所在省|市|区
	private String ggpic;
	private String contact_info;
	
	private Boolean kd100;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Serializable getPK() {
		return this.getId();
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getLogo() {
		return logo;
	}
	public void setLogo(String logo) {
		this.logo = logo;
	}
	public String getWxpay_qr() {
		return wxpay_qr;
	}
	public void setWxpay_qr(String wxpay_qr) {
		this.wxpay_qr = wxpay_qr;
	}
	public String getWxpay_account() {
		return wxpay_account;
	}
	public void setWxpay_account(String wxpay_account) {
		this.wxpay_account = wxpay_account;
	}
	public String getAlipay_qr() {
		return alipay_qr;
	}
	public void setAlipay_qr(String alipay_qr) {
		this.alipay_qr = alipay_qr;
	}
	public String getAlipay_account() {
		return alipay_account;
	}
	public void setAlipay_account(String alipay_account) {
		this.alipay_account = alipay_account;
	}
	public String getPrefix() {
		return prefix;
	}
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	public String getSms_sign() {
		return sms_sign;
	}
	public void setSms_sign(String sms_sign) {
		this.sms_sign = sms_sign;
	}
	public String getSms_key() {
		return sms_key;
	}
	public void setSms_key(String sms_key) {
		this.sms_key = sms_key;
	}
	public String getSms_secret() {
		return sms_secret;
	}
	public void setSms_secret(String sms_secret) {
		this.sms_secret = sms_secret;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public String getGgpic() {
		return ggpic;
	}
	public void setGgpic(String ggpic) {
		this.ggpic = ggpic;
	}
	public String getContact_info() {
		return contact_info;
	}
	public void setContact_info(String contact_info) {
		this.contact_info = contact_info;
	}
	public Boolean getKd100() {
		return kd100;
	}
	public void setKd100(Boolean kd100) {
		this.kd100 = kd100;
	}
	
	
}
