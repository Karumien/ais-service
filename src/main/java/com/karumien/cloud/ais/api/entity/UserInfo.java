/*
 * Copyright (c) 2019-2029 Karumien s.r.o.
 *
 * Karumien s.r.o. is not responsible for defects arising from 
 * unauthorized changes to the source code.
 */
package com.karumien.cloud.ais.api.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Immutable;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * UserInfo entity - information about User.
 *
 * @author <a href="miroslav.svoboda@karumien.com">Miroslav Svoboda</a>
 * @since 1.0, 15. 4. 2019 17:51:09
 */
@Entity
@Table(name = "VIEW_DATZAMEST")
@Immutable
@Data
@EqualsAndHashCode(of = "username")
public class UserInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	private Long id;
 
	@Column(name = "OSCISLO")
	private Integer code;

	@Column(name = "JMENO")
    private String name;
	
	@Column(name = "UZIVJMENO")
    private String username;
	  
	@Column(name = "ROLE_ADMIN")
    private Boolean roleAdmin;

	@Column(name = "ROLE_HIP")
    private Boolean roleHip;

	@Column(name = "FOND")
    private Boolean fond;

	@Column(name = "ID_STREDISKO")
	private Integer department;
}
