/*
 * Copyright (c) 2019-2029 Karumien s.r.o.
 *
 * Karumien s.r.o. is not responsible for defects arising from 
 * unauthorized changes to the source code.
 */
package com.karumien.cloud.ais.api.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * UserInfo entity - information about User.
 *
 * @author <a href="miroslav.svoboda@karumien.com">Miroslav Svoboda</a>
 * @since 1.0, 15. 4. 2019 17:51:09
 */
@Entity
@Table(name = "DATZAMEST")
public class UserInfoEntity {

	@Id
	private Integer id;
 
	@Column(name = "OSCISLO")
	private Integer code;

	@Column(name = "JMENO")
    private String name;
	  
}
