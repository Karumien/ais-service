package com.karumien.cloud.ais.api.entity;

import java.io.Serializable;
import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.karumien.cloud.ais.api.model.WorkDayTypeDTO;
import com.karumien.cloud.ais.api.model.WorkTypeDTO;

import lombok.Data;

@Entity
@Table(name = "AIS_WORK")
@Data
public class Work implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "DATE", nullable = false, insertable = true, updatable = false)
    private LocalDate date;

    @Column(name = "USERNAME", nullable = false, insertable = true, updatable = false)
    private String username;

    @Column(name = "HOURS")
    private Double hours;

    @Column(name = "WORK_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private WorkTypeDTO workType = WorkTypeDTO.NONE;

    @Column(name = "HOURS2")
    private Double hours2;

    @Column(name = "WORK_TYPE2", nullable = false)
    @Enumerated(EnumType.STRING)
    private WorkTypeDTO workType2 = WorkTypeDTO.NONE;

    @Column(name = "WORK_DAY_TYPE", nullable = false, insertable = true, updatable = false)
    @Enumerated(EnumType.STRING)
    private WorkDayTypeDTO workDayType;

}
