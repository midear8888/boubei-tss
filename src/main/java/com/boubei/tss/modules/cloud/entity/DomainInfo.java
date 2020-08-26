package com.boubei.tss.modules.cloud.entity;

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
 * 域信息扩展：必须由域账号自行创建，Admin等域外部创建的账号无效
 * 
 * http://localhost:9000/tss/xdata/json/x_domain?fields=*&name=卜数科技
 * http://localhost:9000/tss/ftl/tabledef?className=com.boubei.tss.modules.cloud.entity.DomainInfo
[
  {'label':'域名','code':'name','nullable':'false'},
  {'label':'缩写','code':'prefix'},
  {'label':'菜单白名单','code':'white_list','jsonUrl':'/tss/auth/module/allresources','multiple':'true'},
  {'label':'菜单黑名单','code':'black_list','jsonUrl':'/tss/auth/module/allresources','multiple':'true'},
  {'label':'账号地登录','code':'multilogin','type':'int'},
  {'label':'logo','code':'logo','type':'file'},
  {'label':'广告位图片','type':'file','code':'ggpic'},
  {'label':'联系方式','code':'contact_info','width':'350','height':'90'},
  {'label':'所处位置','code':'location'},
  
  {'label':'短信签名','code':'sms_sign'}, 
  {'label':'短信账号','code':'sms_key'},
  {'label':'短信秘钥','code':'sms_secret'}, 
  {'label':'短信签名','code':'sms_sign'},
  {'label':'udf1','code':'udf1'},
  {'label':'udf2','code':'udf2'},
  {'label':'udf3','code':'udf3'}
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
	private String prefix; // 订单等取号前缀
	
	private String white_list; // 菜单白名单
	private String black_list; // 菜单黑名单

	/** 短信签名、key、秘钥 */
	private String sms_sign;
	private String sms_key;
	private String sms_secret;
	private String sms_verify;
	private String sms_tosender;
	private String sms_toreceiver;

	private String ggpic;    // 广告位图片
	private String location; // 企业所在省|市|区
	private String udf1;
	private String udf2;
	private String udf3;
	private String type;
	
	/** 收款码图片 */
	private String payment_code; 
	
	/** 返利，eg: 0.12 = 12% */
	private Double rebate; 
	
	/** 账号多地登录 */
	private Integer multilogin = ParamConstants.FALSE;
	
	/** 子域, eg: 'BD1','BD2'  （同过此关联，做报表时允许父域查看子域的数据）*/
	private String sub_domains; 
	
	/** 域组，多个域是一个总公司的分公司，部分数据可以互相看 */
	private String domain_group;

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

	public String getUdf1() {
		return udf1;
	}

	public void setUdf1(String udf1) {
		this.udf1 = udf1;
	}

	public String getUdf2() {
		return udf2;
	}

	public void setUdf2(String udf2) {
		this.udf2 = udf2;
	}

	public String getUdf3() {
		return udf3;
	}

	public void setUdf3(String udf3) {
		this.udf3 = udf3;
	}

	public Integer getMultilogin() {
		return multilogin;
	}

	public void setMultilogin(Integer multilogin) {
		this.multilogin = multilogin;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getWhite_list() {
		return white_list;
	}

	public void setWhite_list(String white_list) {
		this.white_list = white_list;
	}

	public String getBlack_list() {
		return black_list;
	}

	public void setBlack_list(String black_list) {
		this.black_list = black_list;
	}

	public String getSms_verify() {
		return sms_verify;
	}

	public void setSms_verify(String sms_verify) {
		this.sms_verify = sms_verify;
	}

	public String getSms_tosender() {
		return sms_tosender;
	}

	public void setSms_tosender(String sms_tosender) {
		this.sms_tosender = sms_tosender;
	}

	public String getSms_toreceiver() {
		return sms_toreceiver;
	}

	public void setSms_toreceiver(String sms_toreceiver) {
		this.sms_toreceiver = sms_toreceiver;
	}

	public String getSub_domains() {
		return sub_domains;
	}

	public void setSub_domains(String sub_domains) {
		this.sub_domains = sub_domains;
	}

	public String getPayment_code() {
		return payment_code;
	}

	public void setPayment_code(String payment_code) {
		this.payment_code = payment_code;
	}

	public Double getRebate() {
		return rebate;
	}

	public void setRebate(Double rebate) {
		this.rebate = rebate;
	}

	public String getDomain_group() {
		return domain_group;
	}

	public void setDomain_group(String domain_group) {
		this.domain_group = domain_group;
	}
	
}
