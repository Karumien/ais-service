/*
 * Copyright (c) 2019-2029 Karumien s.r.o.
 *
 * Karumien s.r.o. is not responsible for defects arising from 
 * unauthorized changes to the source code.
 */
package com.karumien.cloud.ais.api.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import lombok.Data;

/**
 * Pass Entity - information about pass of User.
 * 
 * @author <a href="miroslav.svoboda@karumien.com">Miroslav Svoboda</a>
 * @since 1.0, 15. 4. 2019 17:40:52 
 */
@Embeddable
@Data
public class ViewUserInfo implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@Column(name = "PERSON_CODE")
	private Integer code;

	@Column(name = "PERSON_NAME")
    private String name;
	  	
	@Column(name = "DEPARTMENT_CODE")
	private String department;

	@Column(name = "USERNAMR")
	private String username;

}
