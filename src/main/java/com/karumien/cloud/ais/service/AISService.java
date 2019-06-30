/*
 * Copyright (c) 2019-2029 Karumien s.r.o.
 *
 * Karumien s.r.o. is not responsible for defects arising from
 * unauthorized changes to the source code.
 */
package com.karumien.cloud.ais.service;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.data.domain.Page;

import com.karumien.cloud.ais.api.entity.UserInfo;
import com.karumien.cloud.ais.api.entity.ViewPass;
import com.karumien.cloud.ais.api.model.PassDTO;
import com.karumien.cloud.ais.api.model.UserInfoDTO;
import com.karumien.cloud.ais.api.model.WorkDayTypeDTO;
import com.karumien.cloud.ais.api.model.WorkHourDTO;
import com.karumien.cloud.ais.api.model.WorkMonthDTO;
import com.karumien.cloud.ais.api.model.WorkTypeDTO;

/**
 * Front AIS Service.
 *
 * @author <a href="miroslav.svoboda@karumien.com">Miroslav Svoboda</a>
 * @since 1.0, 15. 4. 2019 17:09:02
 */
public interface AISService {

    /** Hours in work day 7.5 vs 8.0 */
    double HOURS_IN_DAY = 8d;
    
    default String hours(Double workedHours) {
        if (workedHours == null) {
            return "";
        }
        NumberFormat formatter = new DecimalFormat("#0.00");     
        return formatter.format(workedHours);
    }
    
    default String date(LocalDate date) {
        if (date == null) {
            return "";
        }
        
        return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }
    
    default String days(Number workedHours) {
        if (workedHours == null) {
            return "";
        }
        NumberFormat formatter = new DecimalFormat("#0.00");     
        return formatter.format(workedHours.doubleValue()*AISService.HOURS_IN_DAY);
    }

    default String hoursOnly(@Valid WorkHourDTO work) {
        if (work == null || work.getDate() == null) {
            return "";
        }
        
        return work.getDate().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    default String getDescription(@Valid WorkDayTypeDTO workDayType) {
        switch (workDayType) {
        case NATIONAL_HOLIDAY:
            return "Státní svátek";            
        case WORKDAY:
            return "Pracovní den";
        default:
            return "";
        }
    }
    
    default String getDescription(@Valid WorkTypeDTO workType) {
        switch (workType) {
        case WORK:
            return "Odpracováno";            
        case HOLIDAY:
            return "Dovolená";
        case HOMEOFFICE:
            return "Práce z domova";
        case SICKDAY:
            return "Nemocné dny";
        case SICKNESS:
            return "Lékař";
        case TIMEOFF:
            return "Neplacené volno";
        default:
            return "";
        }
    }

    /**
     * Returns passes filtered by user (optional).
     * 
     * @param usercode
     *            filtered by usercode (optional)
     * @return {@link Page} of {@link PassDTO} filtered by optional user
     */
    Page<ViewPass> getPass(Integer usercode);

    /**
     * Returns passes filtered by user (optional).
     * 
     * @param username
     *            filtered by username (optional)
     * @return {@link Page} of {@link PassDTO} filtered by optional user
     */
    Page<ViewPass> getPass(String username);

    /**
     * Returns all users onsite.
     * 
     * @return {@link List} of {@link PassDTO} which is onsite
     */
    List<ViewPass> getPassOnsite();

    /**
     * Return work month for specified user
     * 
     * @param year
     *            year of work month
     * @param month
     *            year of work month
     * @param username
     *            username records
     * @return {@link WorkMonthDTO} work month of specified user
     */
    WorkMonthDTO getWorkDays(Integer year, Integer month, @NotNull @Valid String username);

    /**
     * Return known users list,
     * 
     * @param username
     * @return {@link List} of {@link UserInfoDTO} known users
     */
    List<UserInfoDTO> getWorkUsers(@Valid String username);

    /**
     * 
     * @param date
     * @param username
     * @param workType
     * @param hours
     * @param workType2
     * @param hours2
     * @param id
     * @return
     */
    @Deprecated
    Long setWork(@NotNull @Valid LocalDate date, @NotNull @Valid String username, @Valid String workType, @Valid String hours, @Valid String workType2,
            @Valid String hours2, @Valid Long id);

    /**
     * Return user by {@code username}.
     * 
     * @param username
     *            username
     * @return {@link UserInfoDTO} selected user by {@code username}
     */
    UserInfo getUser(@Valid String username);

    /**
     * Export workdays to excel sheet
     * 
     * @param year
     *            year of work month
     * @param month
     *            year of work month
     * @param username
     *            username records
     * @param out
     *            {@link OutputStream} for data write
     * @return {@link Workbook} excel export
     * @throws IOException on I/O error
     */
    Workbook exportWorkDays(Integer year, Integer month, @NotNull @Valid String username, OutputStream out) throws IOException;

}
