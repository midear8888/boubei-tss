package com.boubei.tss.modules.cloud.entity;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.boubei.tss.dm.record.ARecordTable;

@Entity
@Table(name = "cloud_proxy_price")
@SequenceGenerator(name = "proxy_price_seq", sequenceName = "proxy_price_seq", initialValue = 1, allocationSize = 10)
public class ProxyPrice extends ARecordTable {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "proxy_price_seq")
	private Long id;

	private String buyer;

	@ManyToOne
	private ModuleDef module;

	private Double price;
	private Double unit_price;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getBuyer() {
		return buyer;
	}

	public void setBuyer(String buyer) {
		this.buyer = buyer;
	}

	public Double getPrice() {
		return price;
	}

	public void setPrice(Double price) {
		this.price = price;
	}

	public ModuleDef getModule() {
		return module;
	}

	public void setModule(ModuleDef module) {
		this.module = module;
	}

	public Serializable getPK() {
		return this.id;
	}

	public Double getUnit_price() {
		return unit_price;
	}

	public void setUnit_price(Double unit_price) {
		this.unit_price = unit_price;
	}

}
