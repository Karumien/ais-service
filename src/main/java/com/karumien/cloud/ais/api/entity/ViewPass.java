/*
 * Copyright (c) 2019-2029 Karumien s.r.o.
 *
 * Karumien s.r.o. is not responsible for defects arising from 
 * unauthorized changes to the source code.
 */
package com.karumien.cloud.ais.api.entity;

import java.io.Serializable;
import java.time.OffsetDateTime;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Immutable;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Pass Entity - information about pass of User.
 * 
 * @author <a href="miroslav.svoboda@karumien.com">Miroslav Svoboda</a>
 * @since 1.0, 15. 4. 2019 17:40:52 
 */
@Entity
@Table(name = "VIEW_DATPRUCHUDAL")
@Data
@EqualsAndHashCode(of = "id")
@Immutable
public class ViewPass implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@Id
    private Long id;

	@Column(name = "ACTION_TYPE")
    private Integer categoryId;

	@Column(name = "ACTION_NAME")
	private String category;

	@Column(name = "CHIP_CODE")
	private String chip;

	@Column(name = "ETIME")
	private OffsetDateTime date;
		
	@Column(name = "DAY")
	private Integer day;

	@Column(name = "MONTH")
	private Integer month;
	
	@Column(name = "YEAR")
	private Integer year;

	@Embedded
    @AttributeOverride(name="code",column=@Column(name="PERSON_CODE"))
    @AttributeOverride(name="name",column=@Column(name="PERSON_NAME"))
    @AttributeOverride(name="department",column=@Column(name="DEPARTMENT_CODE"))
    @AttributeOverride(name="username",column=@Column(name="USERNAME"))
	private ViewUserInfo person;
	
}
