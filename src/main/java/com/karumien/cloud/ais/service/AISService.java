/*
 * Copyright (c) 2019-2029 Karumien s.r.o.
 *
 * Karumien s.r.o. is not responsible for defects arising from
 * unauthorized changes to the source code.
 */
package com.karumien.cloud.ais.service;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
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
import com.karumien.cloud.ais.api.model.WorkDTO;
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
        return formatAsTime(workedHours);
    }

    default String date(LocalDate date) {
        if (date == null) {
            return "";
        }

        return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    default String days(Number workedHours) {
        return formatAsTime(daysInHours(workedHours.doubleValue()));
    }

    default String formatAsTime(Number daysInHours) {
        if (daysInHours == null) {
            return "0:00";
        }
        
        boolean sign = daysInHours.doubleValue() < 0;
        
        double value = Math.abs(daysInHours.doubleValue());
        
        int minutes = (int) Math.floor(60d * (value - Math.floor(value)));
        int hours = (int) Math.floor(value);
        
        return (sign ? "-" : "") + hours + ":" + (minutes < 10 ? "0" : "") + minutes;
    }

    default Number daysInHours(Number workedHours) {
        if (workedHours == null) {
            return BigDecimal.ZERO;
        }
        return workedHours.doubleValue() * AISService.HOURS_IN_DAY;
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
            return "Kancelář";
        case TRIP:
            return "Služební cesta";
        case HOLIDAY:
            return "Dovolená";
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
    List<PassDTO> getPassOnsite();

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
     * @throws IOException
     *             on I/O error
     */
    Workbook exportWorkDays(Integer year, Integer month, @NotNull @Valid String username, OutputStream out) throws IOException;

    /**
     * Update work of selected user.
     * 
     * @param work
     *            work of user
     * @param username
     *            selected username
     */
    void setWork(@Valid WorkDTO work, @NotNull @Valid String username);
    
    default boolean isWorkingType(WorkTypeDTO workType) {
        return workType == WorkTypeDTO.WORK || workType == WorkTypeDTO.SICKNESS || workType == WorkTypeDTO.TRIP;
    }


}
