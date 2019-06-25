/*
 * Copyright (c) 2019-2029 Karumien s.r.o.
 *
 * Karumien s.r.o. is not responsible for defects arising from 
 * unauthorized changes to the source code.
 */
package com.karumien.cloud.ais.api.entity;

import java.io.Serializable;
import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Pass Entity - information about pass of User.
 * 
 * @author <a href="miroslav.svoboda@karumien.com">Miroslav Svoboda</a>
 * @since 1.0, 15. 4. 2019 17:40:52 
 */
@Entity
@Table(name = "DATPRUCHUDAL")
@Data
@EqualsAndHashCode(of = "id")
public class Pass implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @Id
    private Integer id;

    @Column(name = "PRIZTYP")
    private String category;

//    @Column(name = "ACTION_TYPE")
//    private String categoryName;

    @Column(name = "TMKOD")
    private String chip;

    @Column(name = "ETIME")
    private OffsetDateTime date;

}
