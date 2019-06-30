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
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.poi.hssf.usermodel.HeaderFooter;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Footer;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.google.common.base.Objects;
import com.karumien.cloud.ais.api.entity.UserInfo;
import com.karumien.cloud.ais.api.entity.ViewPass;
import com.karumien.cloud.ais.api.entity.Work;
import com.karumien.cloud.ais.api.model.UserInfoDTO;
import com.karumien.cloud.ais.api.model.WorkDTO;
import com.karumien.cloud.ais.api.model.WorkDayDTO;
import com.karumien.cloud.ais.api.model.WorkDayTypeDTO;
import com.karumien.cloud.ais.api.model.WorkHourDTO;
import com.karumien.cloud.ais.api.model.WorkMonthDTO;
import com.karumien.cloud.ais.api.model.WorkTypeDTO;
import com.karumien.cloud.ais.exception.NoDataFoundException;
import com.karumien.cloud.ais.repo.UserInfoRepository;
import com.karumien.cloud.ais.repo.ViewPassRepository;
import com.karumien.cloud.ais.repo.WorkRepository;

/**
 * Service implements {@link AISService}.
 *
 * @author <a href="miroslav.svoboda@karumien.com">Miroslav Svoboda</a>
 * @since 1.0, 15. 4. 2019 17:08:42
 */
@Service
public class AISServiceImpl implements AISService {

    private static final XSSFColor COLOR_AQUA = new XSSFColor(new java.awt.Color(70, 150, 150));
    private static final XSSFColor COLOR_SILVER = new XSSFColor(new java.awt.Color(222, 222, 222));
    private static final XSSFColor COLOR_SHADOW = new XSSFColor(new java.awt.Color(78, 78, 78));
    private static final XSSFColor COLOR_WHITE = new XSSFColor(new java.awt.Color(255, 255, 255));
    private static final int PX = 37;

    @Autowired
    private ViewPassRepository passRepository;

    @Autowired
    private WorkRepository workRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private ModelMapper mapper;

    /** National Holidays */
    private static final List<LocalDate> NATIONAL_HOLIDAYS = Arrays.asList(LocalDate.of(2019, 5, 1),
            LocalDate.of(2019, 5, 8), LocalDate.of(2019, 7, 5), LocalDate.of(2019, 10, 28), LocalDate.of(2019, 12, 24),
            LocalDate.of(2019, 12, 25), LocalDate.of(2019, 12, 26));

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ViewPass> getPass(Integer userCode) {
        return userCode == null ? passRepository.findAll(PageRequest.of(0, 50))
                : passRepository.findByUserCode(userCode, PageRequest.of(0, 50));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<ViewPass> getPassOnsite() {
        return passRepository.findAllOnsite();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ViewPass> getPass(String username) {
        return username == null ? passRepository.findAll(PageRequest.of(0, 50))
                : passRepository.findByUsername(username, PageRequest.of(0, 50));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public WorkMonthDTO getWorkDays(Integer year, Integer month, @NotNull @Valid String username) {

        if (year == null) {
            year = LocalDate.now().getYear();
        }

        if (month == null) {
            month = LocalDate.now().getMonthValue();
        }

        WorkMonthDTO workMonth = new WorkMonthDTO();
        workMonth.setUserInfo(new UserInfoDTO());
        workMonth.getUserInfo().setUsername(username);
        workMonth.setMonth(month);
        workMonth.setYear(year);

        LocalDate dateFrom = LocalDate.of(year, month, 1);
        LocalDate dateTo = dateFrom.with(TemporalAdjusters.lastDayOfMonth());

        List<Work> works = workRepository.findByUsernameAndDateRange(username, dateFrom, dateTo);
        boolean generateWorks = works.isEmpty();

        List<ViewPass> pass = passRepository.findByUsernameAndMonth(username, year, month);

        int sumWorkDays = 0;
        int sumHolidays = 0;
        long sumOnSiteMinutes = 0;

        for (int day = 1; day <= dateTo.getDayOfMonth(); day++) {

            final int daySelected = day;
            LocalDate date = LocalDate.of(year, month, day);

            WorkDayDTO workDay = new WorkDayDTO();
            workDay.setDate(date);
            Work worked = works.stream().filter(w -> w.getDate().equals(date)).findFirst().orElse(null);
            if (worked != null) {
                workDay.setWork(mapper.map(worked, WorkDTO.class));
            }
            workDay.setWorkDayType(getWorkDayType(date));

            List<ViewPass> passDays = pass.stream().filter(p -> p.getDay().equals(daySelected))
                    .collect(Collectors.toList());

            if (workDay.getWorkDayType() == WorkDayTypeDTO.NATIONAL_HOLIDAY) {
                sumHolidays++;
            }

            if (workDay.getWorkDayType() == WorkDayTypeDTO.WORKDAY) {
                sumWorkDays++;
                if (generateWorks) {
                    Work work = new Work();
                    work.setUsername(username);
                    work.setDate(date);
                    work.setHours(AISService.HOURS_IN_DAY);
                    work.setWorkDayType(WorkDayTypeDTO.WORKDAY);
                    work.setWorkType(WorkTypeDTO.WORK);
                    workRepository.save(work);
                    works.add(work);
                }
            }

            ViewPass workStart = null;
            ViewPass lunchStart = null;
            ViewPass lunchEnd = null;
            ViewPass workEnd = null;

            ViewPass lastIn = null;
            long workedMinutes = 0;

            for (ViewPass passDay : passDays) {

                // come in or come to trip
                if ((passDay.getCategoryId() == 1 || passDay.getCategoryId() == 3) && lastIn == null) {
                    lastIn = passDay;
                } else {
                    if (lastIn != null && passDay.getCategoryId() != 1 && passDay.getCategoryId() != 3) {
                        workedMinutes += lastIn.getDate().until(passDay.getDate(), ChronoUnit.MINUTES);
                        lastIn = null;
                    }
                }

                // come from lunch
                if ((passDay.getCategoryId() == 1 || passDay.getCategoryId() == 3) && lunchStart != null
                        && lunchEnd == null) {
                    lunchEnd = passDay;
                }

                // come in first to work
                if ((passDay.getCategoryId() == 1 || passDay.getCategoryId() == 3) && workStart == null) {
                    workStart = passDay;
                }

                // lunch
                if (passDay.getCategoryId() == 7 && lunchStart == null) {
                    lunchStart = passDay;
                }
                // get out from work
                if (passDay.getCategoryId() == 2 && workStart != null) {
                    workEnd = passDay;
                }
            }

            if (workStart != null) {
                workDay.setWorkStart(new WorkHourDTO());
                workDay.getWorkStart().setDate(workStart.getDate());
            } 
            if (workEnd != null) {
                workDay.setWorkEnd(new WorkHourDTO());
                workDay.getWorkEnd().setDate(workEnd.getDate());
            }
            if (lunchStart != null) {
                workDay.setLunchStart(new WorkHourDTO());
                workDay.getLunchStart().setDate(lunchStart.getDate());
            }
            if (lunchEnd != null) {
                workDay.setLunchEnd(new WorkHourDTO());
                workDay.getLunchEnd().setDate(lunchEnd.getDate());
            }

            // corrections
            if (workStart != null && workEnd == null && lastIn != null) {
                workDay.setWorkEnd(new WorkHourDTO());
                workDay.getWorkEnd().setDate(workStart.getDate().plusMinutes((int) AISService.HOURS_IN_DAY * 60 + 30));
                workDay.getWorkEnd().setCorrected(true);
                workEnd = new ViewPass();
                workEnd.setDate(workDay.getWorkEnd().getDate());
                workedMinutes += lastIn.getDate().until(workDay.getWorkEnd().getDate(), ChronoUnit.MINUTES);
            }
                        
            OffsetDateTime defaultWorkEnd = OffsetDateTime.of(LocalDateTime.of(year, month, day, 17, 00),
                    OffsetDateTime.now().getOffset());
            if (workEnd != null && workEnd.getDate().isAfter(defaultWorkEnd)) {
                workedMinutes -= defaultWorkEnd.until(workEnd.getDate(), ChronoUnit.MINUTES);
                workDay.getWorkEnd().setDate(defaultWorkEnd);
                workDay.getWorkEnd().setCorrected(true);
                workEnd = new ViewPass();
                workEnd.setDate(workDay.getWorkEnd().getDate());
            }
            
            if (lunchStart != null && lunchEnd == null) {
                workDay.setLunchEnd(new WorkHourDTO());
                workDay.getLunchEnd().setDate(lunchStart.getDate().plusMinutes(30));
                workDay.getLunchEnd().setCorrected(true);
                workedMinutes -= 30;
            }

            if (lunchStart != null && lunchEnd != null && lunchEnd.getDate().isBefore(lunchStart.getDate().plusMinutes(30))) {
                workedMinutes -= lunchEnd.getDate().until(lunchStart.getDate(), ChronoUnit.MINUTES) + 30;
                lunchEnd.setDate(lunchStart.getDate().plusMinutes(30));
                workDay.setLunchEnd(new WorkHourDTO());
                workDay.getLunchEnd().setDate(lunchEnd.getDate());
                workDay.getLunchEnd().setCorrected(true);
            }
            
            OffsetDateTime defaultLunchStart = OffsetDateTime.of(LocalDateTime.of(year, month, day, 11, 0),
                    OffsetDateTime.now().getOffset());
            
            if (workStart != null && lunchStart == null && lunchEnd == null && workEnd != null  
                    && workStart.getDate().until(workEnd.getDate(), ChronoUnit.MINUTES) >= 4*60+30) {
                
                if (workStart.getDate().isAfter(defaultLunchStart) || workEnd.getDate().isBefore(defaultLunchStart)) {
                    defaultLunchStart = workStart.getDate().plusHours(2).truncatedTo(ChronoUnit.HOURS);
                }
                
                workDay.setLunchStart(new WorkHourDTO());
                workDay.getLunchStart().setDate(defaultLunchStart);
                workDay.getLunchStart().setCorrected(true);
                workDay.setLunchEnd(new WorkHourDTO());
                workDay.getLunchEnd().setDate(workDay.getLunchStart().getDate().plusMinutes(30));
                workDay.getLunchEnd().setCorrected(true);
                workedMinutes -= 30;
            }
            
            int dayLightMinutes = month >= 6 && month <= 8 ? 0 : 30;
            OffsetDateTime realStart = OffsetDateTime.of(LocalDateTime.of(year, month, day, 6, dayLightMinutes), OffsetDateTime.now().getOffset());
            if (workStart != null && workStart.getDate().isBefore(realStart)) {
                workDay.getWorkStart().setCorrected(true);
                workedMinutes -= workDay.getWorkStart().getDate().until(realStart, ChronoUnit.MINUTES);
                workDay.getWorkStart().setDate(realStart);
            }

            workDay.setWorkedHours(workStart == null ? null
                    : BigDecimal.valueOf(workedMinutes / 60d).setScale(2, RoundingMode.FLOOR).doubleValue());
            sumOnSiteMinutes += workedMinutes;
            workMonth.addWorkDaysItem(workDay);
        }

        Map<WorkTypeDTO, WorkDTO> sums = new HashMap<>();
        works.stream().filter(w -> w.getWorkType() != null && w.getWorkType() != WorkTypeDTO.NONE
                && w.getHours() != null && w.getHours() > 0).forEach(w -> {
                    WorkDTO sum = sums.get(w.getWorkType());
                    if (sum == null) {
                        sum = new WorkDTO();
                        sum.setWorkType(w.getWorkType());
                        sum.setHours(0d);
                        sums.put(w.getWorkType(), sum);
                    }
                    sum.setHours(sum.getHours().doubleValue() + w.getHours().doubleValue());
                });
        works.stream().filter(w -> w.getWorkType2() != null && w.getWorkType2() != WorkTypeDTO.NONE
                && w.getHours2() != null && w.getHours2() > 0).forEach(w -> {
                    WorkDTO sum = sums.get(w.getWorkType2());
                    if (sum == null) {
                        sum = new WorkDTO();
                        sum.setWorkType(w.getWorkType2());
                        sum.setHours(0d);
                        sums.put(w.getWorkType2(), sum);
                    }
                    sum.setHours(sum.getHours().doubleValue() + w.getHours2().doubleValue());
                });

        workMonth.setSums(new ArrayList<>(sums.values()));
        workMonth.setSumHolidays(sumHolidays);
        workMonth.setSumWorkDays(sumWorkDays);
        workMonth.setSumOnSiteDays(BigDecimal.valueOf(sumOnSiteMinutes / 60d / AISService.HOURS_IN_DAY)
                .setScale(2, RoundingMode.FLOOR).doubleValue());
        return workMonth;
    }

    private WorkDayTypeDTO getWorkDayType(LocalDate date) {

        if (NATIONAL_HOLIDAYS.contains(date)) {
            return WorkDayTypeDTO.NATIONAL_HOLIDAY;
        }

        if (date.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return WorkDayTypeDTO.SATURDAY;
        }

        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return WorkDayTypeDTO.SUNDAY;
        }

        return WorkDayTypeDTO.WORKDAY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserInfoDTO> getWorkUsers(@Valid String username) {

        UserInfo currentUser = getUser(username);
        
        if (Boolean.TRUE.equals(currentUser.getRoleAdmin()) || Boolean.TRUE.equals(currentUser.getRoleHip())) {
            List<UserInfoDTO> users = (Boolean.TRUE.equals(currentUser.getRoleAdmin()) ? 
                userInfoRepository.findAllOrderByUsername() : userInfoRepository.findAllOrderByUsernameForHip(currentUser.getDepartment()))
                    .stream().map(user -> mapper.map(user, UserInfoDTO.class)).collect(Collectors.toList());
                    
            users.forEach(u -> u.setSelected(u.getUsername().equals(username)));
            return users;
        }
        
        return Arrays.asList(mapper.map(currentUser, UserInfoDTO.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void setWork(@Valid WorkDTO work, @NotNull @Valid String username) {
        
        Optional<Work> workSaved = workRepository.findById(work.getId());
        if (!workSaved.isPresent()) {
            return;
        }

        Work workUpdated = workSaved.get();
        if (!Objects.equal(workUpdated.getUsername(), username)) {
            return;
        }

        mapper.map(work, workUpdated);
        workRepository.save(workUpdated);

    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public UserInfo getUser(@Valid String username) {
        return userInfoRepository.findByUsername(username)
                .orElseThrow(() -> new NoDataFoundException("NO.USER", "No User for USERNAME = " + username));   
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Workbook exportWorkDays(Integer year, Integer month, @NotNull @Valid String username, OutputStream out) throws IOException {
        
        UserInfoDTO selectedUser = mapper.map(getUser(username), UserInfoDTO.class);

        double fond = selectedUser.getFond() != null ? selectedUser.getFond() / 100d : 1d;
        
        XSSFWorkbook workbook = new XSSFWorkbook();
        Map<ExcelStyleType, CellStyle> styles = prepareStyles(workbook, true);

        String yearmonth = year + "." + (month < 10 ? "0":"") + month;
        
        Sheet sheet = workbook.createSheet(yearmonth + " " + username);

        int row = 0;
        int k = 0;

        sheet.setColumnWidth(k++, 120 * PX);
        sheet.setColumnWidth(k++, 150 * PX);
        sheet.setColumnWidth(k++, 70 * PX);
        sheet.setColumnWidth(k++, 100 * PX);
        sheet.setColumnWidth(k++, 70 * PX);
        sheet.setColumnWidth(k++, 70 * PX);
        sheet.setColumnWidth(k++, 70 * PX);
        sheet.setColumnWidth(k++, 100 * PX);

        createSimpleRow(sheet, row++, styles.get(ExcelStyleType.H1), 
                yearmonth, selectedUser.getName(),
                null, null, null, null, null, "ev.č. " + selectedUser.getCode());               
        
        row++;
        createSimpleRow(sheet, row++, styles.get(ExcelStyleType.TH), 
                "datum", "kategorie", "příchod", "oběd od-do", "odchod", "celkem", "výkazy", "");

        WorkMonthDTO workMonthDTO = getWorkDays(year, month, username);
        for (WorkDayDTO workDay : workMonthDTO.getWorkDays()) {

            WorkDTO work = workDay.getWork();
            
            createSimpleRow(sheet, row++, styles.get(ExcelStyleType.TD), styles.get(ExcelStyleType.TD_PRICE),    
                    date(workDay.getDate()), getDescription(workDay.getWorkDayType()), hoursOnly(workDay.getWorkStart()), 
                    hoursOnly(workDay.getLunchStart()) + (workDay.getLunchStart() != null ? " - " : "") +  hoursOnly(workDay.getLunchEnd()), 
                    hoursOnly(workDay.getWorkEnd()), workDay.getWorkedHours(),                     
                    work != null ? work.getHours() : "", work != null ? getDescription(work.getWorkType()) : "");

            createSimpleRow(sheet, row++, styles.get(ExcelStyleType.VALUE), styles.get(ExcelStyleType.VALUE_PRICE),    
                    "", "", "", "", "", "", 
                    work != null ? work.getHours2() : "", work != null ? getDescription(work.getWorkType2()) : "");
        }        

        row++;
        
        createSimpleRow(sheet, row++, styles.get(ExcelStyleType.TH), 
                "Souhrn", "");

        int index = 0;

        // Fond
        Row rowCell = sheet.createRow(row++);
        Cell cell = rowCell.createCell(index++, CellType.STRING);
        cell.setCellStyle(styles.get(ExcelStyleType.TD));
        cell.setCellValue("Fond");
        
        cell = rowCell.createCell(index++, CellType.NUMERIC);
        cell.setCellStyle(styles.get(ExcelStyleType.TD_PRICE));
        cell.setCellValue((selectedUser.getFond() == null ? workMonthDTO.getSumWorkDays() : workMonthDTO.getSumWorkDays() * fond) * HOURS_IN_DAY);

        // Svátky
        index = 0;
        rowCell = sheet.createRow(row++);
        cell = rowCell.createCell(index++, CellType.STRING);
        cell.setCellStyle(styles.get(ExcelStyleType.TD));
        cell.setCellValue("Svátky");
        
        cell = rowCell.createCell(index++, CellType.NUMERIC);
        cell.setCellStyle(styles.get(ExcelStyleType.TD_PRICE));
        cell.setCellValue(workMonthDTO.getSumHolidays() * HOURS_IN_DAY);

        // Aditus
        index = 0;
        rowCell = sheet.createRow(row++);
        cell = rowCell.createCell(index++, CellType.STRING);
        cell.setCellStyle(styles.get(ExcelStyleType.TD));
        cell.setCellValue("Aditus");
        
        cell = rowCell.createCell(index++, CellType.NUMERIC);
        cell.setCellStyle(styles.get(ExcelStyleType.TD_PRICE));
        cell.setCellValue(workMonthDTO.getSumOnSiteDays() * HOURS_IN_DAY);
        
        for (WorkDTO work : workMonthDTO.getSums()) {
            index = 0;
            rowCell = sheet.createRow(row++);
            cell = rowCell.createCell(index++, CellType.STRING);
            cell.setCellStyle(styles.get(ExcelStyleType.TD));
            cell.setCellValue(getDescription(work.getWorkType()));
            
            cell = rowCell.createCell(index++, CellType.NUMERIC);
            cell.setCellStyle(styles.get(ExcelStyleType.TD_PRICE));
            cell.setCellValue(work.getHours() == null ? null : work.getHours());
        }
        

        sheet.createFreezePane(2, 3);
        sheet.setRepeatingRows(CellRangeAddress.valueOf("A1:H3"));

        createFooter(sheet, false);
        sheet.setFitToPage(true);
        
        if (out != null) {
            workbook.write(out);
        }
        
        return workbook;    
    }

    private void createSimpleRow(Sheet sheet, int rowIndex, CellStyle style, Object... values) {
        createSimpleRow(sheet, rowIndex, style, style, values);
    }
    
    private void createSimpleRow(Sheet sheet, int rowIndex, CellStyle style, CellStyle right, Object... values) {
        Row row = sheet.createRow(rowIndex);

        int index = 0;
        boolean alignLeft = true;

        for (Object value : values) {

            if (value == null) {
                Cell cell = row.createCell(index, CellType.BLANK);
                cell.setCellStyle(style);
                index++;
                continue;
            }

            Cell cell = row.createCell(index, value instanceof Number ? CellType.NUMERIC : CellType.STRING);
            cell.setCellStyle(style);

            String svalue = value == null ? "" : "" + value;

            if (value instanceof Boolean) {
                svalue = Boolean.TRUE.equals(value) ? "1" : "0";
            }

            if (value instanceof Number) {
                alignLeft = false;
                cell.setCellValue(((Number) value).doubleValue());
                cell.setCellStyle(alignLeft ? style : right);
            } else {
                if (svalue != null && !StringUtils.isEmpty(value)) {
                    cell.setCellValue(svalue);
                }
            }

            index++;
        }
    }

    
    private Map<ExcelStyleType, CellStyle> prepareStyles(XSSFWorkbook workbook, boolean wrapText) {

        Map<ExcelStyleType, CellStyle> styles = new EnumMap<>(ExcelStyleType.class);

        Font fontNor = workbook.createFont();
        fontNor.setFontHeightInPoints((short) 18);
        fontNor.setFontName("Verdana");
        fontNor.setColor(IndexedColors.GREY_80_PERCENT.getIndex());

        Font fontFet = workbook.createFont();
        fontFet.setFontHeightInPoints((short) 9);
        fontNor.setFontName("Verdana");
        fontFet.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

        Font fontFetTH = workbook.createFont();
        fontFetTH.setFontHeightInPoints((short) 9);
        fontNor.setFontName("Verdana");
        fontFetTH.setColor(IndexedColors.WHITE.getIndex());

        Font fontFetTH1 = workbook.createFont();
        fontFetTH1.setFontHeightInPoints((short) 11);
        fontNor.setFontName("Verdana");
        fontFetTH1.setColor(IndexedColors.WHITE.getIndex());

        Font fontValue = workbook.createFont();
        fontValue.setFontHeightInPoints((short) 11);
        fontNor.setFontName("Verdana");
        fontValue.setColor(IndexedColors.GREY_80_PERCENT.getIndex());

        XSSFCellStyle styleH1 = workbook.createCellStyle();
        styleH1.setFont(fontNor);
        styleH1.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styleH1.setFillForegroundColor(COLOR_WHITE);
        styles.put(ExcelStyleType.H1, styleH1);

        XSSFCellStyle styleH1Right = workbook.createCellStyle();
        styleH1Right.setFont(fontNor);
        styleH1Right.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styleH1Right.setFillForegroundColor(COLOR_WHITE);
        styleH1Right.setAlignment(HorizontalAlignment.RIGHT);
        styles.put(ExcelStyleType.H1_RIGHT, styleH1Right);

        XSSFCellStyle styleH2 = workbook.createCellStyle();
        styleH2.setFont(fontFet);
        styleH2.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styleH2.setFillForegroundColor(COLOR_SILVER);
        styles.put(ExcelStyleType.H2, styleH2);

        XSSFCellStyle styleTH = workbook.createCellStyle();
        styleTH.setFont(fontFetTH);
        styleTH.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styleTH.setFillForegroundColor(COLOR_AQUA);
        styleTH.setWrapText(wrapText);
        styles.put(ExcelStyleType.TH, styleTH);

        XSSFCellStyle styleTHRight = workbook.createCellStyle();
        styleTHRight.setFont(fontFetTH);
        styleTHRight.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styleTHRight.setFillForegroundColor(COLOR_AQUA);
        styleTHRight.setWrapText(wrapText);
        styleTHRight.setAlignment(HorizontalAlignment.RIGHT);
        styles.put(ExcelStyleType.TH_RIGHT, styleTHRight);

        XSSFCellStyle styleTH1 = workbook.createCellStyle();
        styleTH1.setFont(fontFetTH1);
        styleTH1.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styleTH1.setFillForegroundColor(COLOR_AQUA);
        styleTH1.setWrapText(false);
        styles.put(ExcelStyleType.TH1, styleTH1);

        XSSFCellStyle styleTHRight1 = workbook.createCellStyle();
        styleTHRight1.setFont(fontFetTH1);
        styleTHRight1.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styleTHRight1.setFillForegroundColor(COLOR_AQUA);
        styleTHRight1.setWrapText(false);
        styleTHRight1.setAlignment(HorizontalAlignment.RIGHT);
        styles.put(ExcelStyleType.TH1_RIGHT, styleTHRight1);

        XSSFCellStyle styleValue = workbook.createCellStyle();
        styleValue.setFont(fontValue);
        styleValue.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styleValue.setFillForegroundColor(COLOR_WHITE);
        styleValue.setBorderBottom(BorderStyle.THIN);
        styleValue.setBottomBorderColor(COLOR_SHADOW);
        styles.put(ExcelStyleType.VALUE, styleValue);

        XSSFCellStyle styleValueDate = workbook.createCellStyle();
        styleValueDate.setFont(fontValue);
        styleValueDate.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styleValueDate.setFillForegroundColor(COLOR_WHITE);
        styleValueDate.setDataFormat(workbook.createDataFormat().getFormat("dd.MM.yyyy"));
        styleValueDate.setBorderBottom(BorderStyle.THIN);
        styleValueDate.setBottomBorderColor(COLOR_WHITE);
        styleValueDate.setAlignment(HorizontalAlignment.LEFT);
        styles.put(ExcelStyleType.VALUE_DATE, styleValueDate);

        XSSFCellStyle styleTD = workbook.createCellStyle();
        styleTD.setFont(fontValue);
        styleTD.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styleTD.setFillForegroundColor(COLOR_WHITE);
        styleTD.setWrapText(false);
//        styleTD.setBorderBottom(BorderStyle.THIN);
//        styleTD.setBottomBorderColor(COLOR_SILVER);
        styles.put(ExcelStyleType.TD, styleTD);

        XSSFCellStyle styleTDPrice = workbook.createCellStyle();
        styleTDPrice.setFont(fontValue);
        styleTDPrice.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styleTDPrice.setFillForegroundColor(COLOR_WHITE);
        styleTDPrice.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        styleTDPrice.setAlignment(HorizontalAlignment.RIGHT);
//        styleTDPrice.setBorderBottom(BorderStyle.THIN);
//        styleTDPrice.setBottomBorderColor(COLOR_SILVER);
        styles.put(ExcelStyleType.TD_PRICE, styleTDPrice);

        XSSFCellStyle styleValuePrice = workbook.createCellStyle();
        styleValuePrice.setFont(fontValue);
        styleValuePrice.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styleValuePrice.setFillForegroundColor(COLOR_WHITE);
        styleValuePrice.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        styleValuePrice.setAlignment(HorizontalAlignment.RIGHT);
        styleValuePrice.setBorderBottom(BorderStyle.THIN);
        styleValuePrice.setBottomBorderColor(COLOR_SHADOW);
        styles.put(ExcelStyleType.VALUE_PRICE, styleValuePrice);

        XSSFCellStyle styleTDDate = workbook.createCellStyle();
        styleTDDate.setFont(fontValue);
        styleTDDate.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styleTDDate.setFillForegroundColor(COLOR_WHITE);
        styleTDDate.setDataFormat(workbook.createDataFormat().getFormat("dd.MM.yyyy"));
        styleTDDate.setBottomBorderColor(COLOR_SILVER);
        styleTDDate.setAlignment(HorizontalAlignment.RIGHT);
        styleTDDate.setBorderBottom(BorderStyle.THIN);
        styleTDDate.setBottomBorderColor(COLOR_SILVER);
        styles.put(ExcelStyleType.TD_DATE, styleTDDate);

        return styles;
    }

    
    private void createFooter(Sheet sheet, boolean landscape) {
        sheet.setMargin(Sheet.RightMargin, 0.25);
        sheet.setMargin(Sheet.LeftMargin, 0.25);
        sheet.setMargin(Sheet.TopMargin, 0.75);
        sheet.setMargin(Sheet.BottomMargin, 0.75);
        sheet.setAutobreaks(true);

        PrintSetup ps = sheet.getPrintSetup();
        ps.setLandscape(landscape);
        ps.setPaperSize(PrintSetup.A4_PAPERSIZE);
        ps.setFitWidth((short) 0);
        ps.setFitHeight((short) 1);

        Footer footer = sheet.getFooter();
        footer.setLeft("© 2010-2020 Karumien s.r.o. - na základě licence i24 pro web/informační systém.");
        footer.setRight("Strana " + HeaderFooter.page() + " z " + HeaderFooter.numPages());
    }


}
