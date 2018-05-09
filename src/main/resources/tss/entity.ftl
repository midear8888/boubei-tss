package com.boubei.demo.crud;

import java.util.Date;
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
@Table(name = "${tableName}")
@SequenceGenerator(name = "${tableName}_seq", sequenceName = "${tableName}_seq", initialValue =1, allocationSize = 10)
public class DemoEntity extends ARecordTable  {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "${tableName}_seq")
	private Long   	id;
    
    ${fields};
    
    public Serializable getPK() {
		return this.id;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
}
