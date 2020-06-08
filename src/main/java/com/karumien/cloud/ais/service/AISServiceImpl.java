/*
 * Copyright (c) 2019-2029 Karumien s.r.o.
 *
 * Karumien s.r.o. is not responsible for defects arising from 
 * unauthorized changes to the source code.
 */
package com.karumien.cloud.ais.service;

import java.io.IOException;
import java.io.OutputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetProtection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.google.common.base.Objects;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.karumien.client.adochazka.schemas.CustomerDataDen;
import com.karumien.client.adochazka.schemas.Oddeleni;
import com.karumien.client.adochazka.schemas.Pristup;
import com.karumien.client.adochazka.schemas.Pritomnost;
import com.karumien.client.adochazka.schemas.Uzivatel;
import com.karumien.client.adochazka.service.ADochazkaService;
import com.karumien.cloud.ais.api.entity.UserInfo;
import com.karumien.cloud.ais.api.entity.ViewPass;
import com.karumien.cloud.ais.api.entity.Work;
import com.karumien.cloud.ais.api.model.PassDTO;
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
    private ADochazkaService aDochazkaService;

    @Autowired
    private ModelMapper mapper;

    /** National Holidays */
    private static final List<LocalDate> NATIONAL_HOLIDAYS = Arrays.asList(LocalDate.of(2019, 5, 1),
        LocalDate.of(2019, 5, 8), LocalDate.of(2019, 7, 5), LocalDate.of(2019, 10, 28), 
        LocalDate.of(2019, 11, 17), 
        LocalDate.of(2019, 12, 24), LocalDate.of(2019, 12, 25), LocalDate.of(2019, 12, 26),
        LocalDate.of(2020, 1, 1), LocalDate.of(2020, 4, 10), LocalDate.of(2020, 04, 13),
        LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 8), LocalDate.of(2020, 7, 6),
        LocalDate.of(2020, 9, 28), LocalDate.of(2020, 10, 28), LocalDate.of(2020, 11, 17),
        LocalDate.of(2020, 12, 24), LocalDate.of(2020, 12, 25), LocalDate.of(2020, 12, 26));

    private static final Map<String, String> TRANSLATES = new HashMap<>();
    static {
        TRANSLATES.put("Prace", "V kanceláři");
        TRANSLATES.put("SluzebniCesta", "Služební cesta");
        TRANSLATES.put("Prestavka", "Přestávka/Oběd");
        TRANSLATES.put("Odchod", "Odchod z práce");
        TRANSLATES.put("Lekar", "Lékař");
        TRANSLATES.put("Pohreb", "Pohřeb");
        TRANSLATES.put("Svatba", "Svatba");
        TRANSLATES.put("OTISK_PRSTU", "Otisk");
        TRANSLATES.put("CIPOVA_KARTA", "Čip");
    }

    private static final Map<Integer, String> KEYBOARD = new HashMap<>();
    static {
        KEYBOARD.put(Integer.valueOf(0), "<neznámý>");
        KEYBOARD.put(Integer.valueOf(1), "Příchod");
        KEYBOARD.put(Integer.valueOf(2), "Odchod");
        KEYBOARD.put(Integer.valueOf(3), "Přestávka/Oběd");
        KEYBOARD.put(Integer.valueOf(4), "Služební cesta");
        KEYBOARD.put(Integer.valueOf(5), "Lékař");
    }

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
    @Cacheable(value = "online")
    public List<PassDTO> getPassOnsite() {
        
        List<PassDTO> onsite = new ArrayList<>(); 
        Set<Integer> ids = new HashSet<>();
        
        for (Pritomnost p : aDochazkaService.getActualWorkers()) {
            
            UserInfoDTO user = new UserInfoDTO();
            user.setName(p.getUzivatelJmeno().getValue() + " " + gdpr(p.getUzivatelPrijmeni().getValue()));
            user.setCode(toInt(p.getUzivatelCislo().getValue()));
            user.setId(user.getCode());            
            user.setDepartment(p.getOddeleniString().getValue());

            ids.add(user.getId());
            
            PassDTO pass = new PassDTO();
            pass.setDate(aDochazkaService.toOffsetDateTime(p.getPrichod().getValue()));
            pass.setCategory(toCategory(p.getCinnostNazev().getValue()));
            pass.setCategoryId(toCategoryId(p.getCinnostNazev().getValue()));
            pass.setPerson(user);

            onsite.add(pass);
        }

        onsite.addAll(findAllLeaved().stream().filter(u -> !ids.contains(u.getPerson().getId())).collect(Collectors.toList()));
        
        Collections.sort(onsite, new Comparator<PassDTO>() {

            @Override
            public int compare(PassDTO o1, PassDTO o2) {
                if (!o1.getCategoryId().equals(o2.getCategoryId())) {
                    return o1.getCategoryId().compareTo(o2.getCategoryId());
                }
                return o1.getDate().compareTo(o2.getDate());
            }
            
        });
        return onsite;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PassDTO> getAccesses(LocalDate day, String username) {        

        OffsetDateTime today = day.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());

        Uzivatel uzivatel = getUzivatel(username);
        List<PassDTO> passes = new ArrayList<>();
        
        if (uzivatel == null || uzivatel.getId() == null) {
            return passes;
        }
                
        // FIXME: Performance - dayTo + typ cinnosti
        List<Pristup> pristupy = aDochazkaService.getAccesses(today)
            .stream().filter(p -> !p.getUzivatelId().isNil() && p.getUzivatelId().getValue().equals(uzivatel.getId()))
            .filter(p -> p.getDatum().getYear() == day.getYear() && p.getDatum().getMonth() == day.getMonthValue() && p.getDatum().getDay() == day.getDayOfMonth())
            .collect(Collectors.toList());
        
        for (Pristup pristup : pristupy) {
            
            UserInfoDTO user = new UserInfoDTO();
            user.setName(uzivatel.getJmeno().getValue() + " " + gdpr(uzivatel.getPrijmeni().getValue()));
            user.setCode(uzivatel.getId());
            user.setId(user.getCode());
            
            Oddeleni oddeleni = uzivatel.getOddeleni().getValue().getOddeleni().stream().findFirst().orElse(null); 
            user.setDepartment(oddeleni != null ? oddeleni.getNazev().getValue(): null);
                        
            PassDTO pass = new PassDTO();
            pass.setId(pristup.getId());
            pass.setDate(aDochazkaService.toOffsetDateTime(pristup.getDatum()));
            
            Integer klavesa = pristup.getKlavesa1().isNil() ? 0 : pristup.getKlavesa1().getValue();
            pass.setCategoryId(klavesa);
            pass.setCategory(KEYBOARD.get(klavesa));
            pass.setPerson(user);
            pass.setChip(TRANSLATES.get(pristup.getTypVerifikace().toString()));
            passes.add(pass);
        }                    
                    
        return passes;
    }
        
    public List<PassDTO> findAllLeaved() {
        
        OffsetDateTime today = LocalDate.now().atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        Map<String, Uzivatel> users = aDochazkaService.getWorkersMap();
        
        List<PassDTO> found = new ArrayList<>();
        
        List<Pristup> accesses = aDochazkaService.getAccesses(today);
        Collections.reverse(accesses);
        Set<String> ids = new HashSet<>();

        for (Pristup p : accesses) {

            String id = p.getCisloUzivatele().getValue();            
            if (!p.getKlavesa1().isNil() && p.getKlavesa1().getValue() != 1 && !ids.contains(id)) {
            
                ids.add(id);
                Uzivatel u = users.get(id);
                
                UserInfoDTO user = new UserInfoDTO();
                user.setName(u.getJmeno().getValue() + " " + gdpr(u.getPrijmeni().getValue()));
                user.setCode(toInt(id));
                user.setId(user.getCode());
                
                Oddeleni oddeleni = u.getOddeleni().getValue().getOddeleni().stream().findFirst().orElse(null); 
                user.setDepartment(oddeleni != null ? oddeleni.getNazev().getValue(): null);
            
                PassDTO pass = new PassDTO();
                pass.setDate(aDochazkaService.toOffsetDateTime(p.getDatum()));
                pass.setCategory(toCategory("Odchod"));
                pass.setCategoryId(120);
                pass.setPerson(user);
    
                found.add(pass);
            }            
        }
        
        return found;
        
    }

    private static String gdpr(String value) {
        return value == null ? null : value.substring(0, 1) + (value.contains("ö") ? "ö" : "") + ".";
    }

    private static String toCategory(String value) {
        return TRANSLATES.containsKey(value) ? TRANSLATES.get(value) : value; 
    }

    private static Integer toCategoryId(String value) {
        return "Prace".equals(value) ? 1 : 100;
    }

    private static Integer toInt(String value) {
        try {
            if (value != null) {
                return Integer.valueOf(value);
            }
        } catch (Exception e) {
        }
        return null;
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
    @Transactional(readOnly = true)
    public Uzivatel getUzivatel(String username) {
        UserInfo user = getUser(username);
        return aDochazkaService.getWorkersMap().get("" + user.getCode());
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

        int sumWorkDays = 0;
        int sumHolidays = 0;
        double sumWork = 0;
       
        Uzivatel uzivatel = getUzivatel(username);
        
        Map<Integer, CustomerDataDen> workMonthMap =
            uzivatel == null ? new HashMap<>() :
                aDochazkaService.getWorkMonthMap(year, month, uzivatel.getId());
                
        for (int day = 1; day <= dateTo.getDayOfMonth(); day++) {
            
          OffsetDateTime globalStart = OffsetDateTime.of(year, month, day, 6, 
                  (month < 6 && year >= 2020) ? 30 : 0, 0, 0, OffsetDateTime.now().getOffset());  
          OffsetDateTime globalEnd = OffsetDateTime.of(year, month, day, 17, 00, 0, 0, OffsetDateTime.now().getOffset());  
            
          WorkDayDTO workDay = new WorkDayDTO();
          LocalDate date = LocalDate.of(year, month, day);
          workDay.setDate(date);
          workDay.setUnpaid(0d);

          CustomerDataDen den = workMonthMap.get(day);
          workDay.setWorkDayType(getWorkDayType(date, den));                        
          
          if (workDay.getWorkDayType() == WorkDayTypeDTO.NATIONAL_HOLIDAY) {
              sumHolidays++;
          }
          
          if (workDay.getWorkDayType() == WorkDayTypeDTO.WORKDAY) {
            sumWorkDays++;
            if (generateWorks) {
                Work work = new Work();
                
                work.setUsername(username);
                work.setDate(date);
                work.setWorkDayType(WorkDayTypeDTO.WORKDAY);

                // contractors
                if (den == null) {
                  work.setHours(AISService.HOURS_IN_DAY);
                  work.setWorkType(WorkTypeDTO.WORK);
                }
                    
                works.add(workRepository.save(work));
            }
          }

          if (den != null && workDay.getWorkDayType() == WorkDayTypeDTO.WORKDAY) {
              
              OffsetDateTime skutecnyPrichod = aDochazkaService.toOffsetDateTime(den.getSkutecnyPrichod().isNil() ? 
                      null : den.getSkutecnyPrichod().getValue());
              OffsetDateTime skutecnyOdchod = aDochazkaService.toOffsetDateTime(den.getSkutecnyOdchod().isNil() ? 
                      null : den.getSkutecnyOdchod().getValue());
              
              WorkHourDTO ws = new WorkHourDTO();
              ws.setDate(skutecnyPrichod);
              workDay.setWorkStart(ws);
              
              WorkHourDTO we = new WorkHourDTO();
              we.setDate(skutecnyOdchod);
              workDay.setWorkEnd(we);

              workDay.setSick(den.getCelkemLekar() + den.getCelkemNemoc() + den.getCelkemSickDay());
              workDay.setTrip(den.getCelkemSluzebniCesta() != null ? den.getCelkemSluzebniCesta() : 0);
              workDay.setPayed(
                  (den.getCelkemSvatba() != null ? den.getCelkemSvatba() : 0) +
                  (den.getCelkemPohreb() != null ? den.getCelkemPohreb() : 0));

              workDay.setWorkedHours(den.getCelkemPrace() + workDay.getSick());              
              workDay.setSaldo(den.getBalanc());
              
              if (month >= 6 && year >= 2020) {
                  workDay.setWorkedHours(den.getCelkemNaPracovisti() + den.getCelkemSluzebniCesta());              
                  workDay.setSaldo(workDay.getWorkedHours()-8);
              }
                            
              // today
              if (LocalDate.now().equals(workDay.getDate())) {
                  workDay.setSaldo(0d);
              }              
              
              workDay.setLunch(den.getCelkemPrestavka());

//              if (workDay.getDate().getDayOfMonth()==17) {
//                  System.out.println("dd");
//              }
              
              // correct lunch
              if (((workDay.getLunch() == null || workDay.getLunch() == 0) && den.getCelkemPrace() > 4.5d || workDay.getLunch() < 0.5 && workDay.getLunch() > 0 ) ) {
                  workDay.setOriginalLunch(workDay.getLunch() != null ? workDay.getLunch() : 0);
                  workDay.setLunch(0.5d);
                  workDay.setWorkedHours(workDay.getWorkedHours() - 0.5d);              
                  workDay.setSaldo(den.getBalanc() - 0.5d);
              }              
              
              OffsetDateTime prichod = aDochazkaService.toOffsetDateTime(den.getPrichod().isNil() ? 
                      null : den.getPrichod().getValue());
              OffsetDateTime odchod = aDochazkaService.toOffsetDateTime(den.getOdchod().isNil() ? null : den.getOdchod().getValue());
              
              if (prichod != null && prichod.isBefore(globalStart) 
                  || workDay.getWorkStart() != null && workDay.getWorkStart().getDate() != null 
                  && workDay.getWorkStart().getDate().isBefore(globalStart)) {
                  workDay.setUnpaid(workDay.getUnpaid() + ChronoUnit.SECONDS.between(prichod, globalStart) / 3600d);
                  prichod = globalStart;
              }
              
              if (isDifferent(workDay.getWorkStart().getDate(), prichod)) {
                  workDay.getWorkStart().setOriginal(workDay.getWorkStart().getDate());
                  workDay.getWorkStart().setDate(prichod);
                  workDay.getWorkStart().setCorrected(true);
              }
              
              if (isDifferent(workDay.getWorkEnd().getDate(), odchod)) {
                  workDay.getWorkEnd().setOriginal(workDay.getWorkEnd().getDate());
                  workDay.getWorkEnd().setDate(odchod);
                  workDay.getWorkEnd().setCorrected(odchod == null);
              }
              
              boolean fixed = false;
              
              // fix last 
              if (prichod != null && skutecnyOdchod == null) {// && (workDay.getTrip().doubleValue() > 0 || workDay.getSick() > 0)) {
                  
                  List<PassDTO> accesses = getAccesses(workDay.getDate(), username);
                  PassDTO lastCat = accesses.size() > 0 ? accesses.get(accesses.size()-1) : null;
                  if (lastCat != null && (lastCat.getCategoryId() == 4 || lastCat.getCategoryId() == 5)) {
                      workDay.getWorkEnd().setOriginal(odchod);
                      odchod = prichod.plusMinutes((long) (workDay.getLunch() * 60d)).plusHours(8);
                      workDay.getWorkEnd().setDate(odchod);
                      workDay.getWorkEnd().setCorrected(true);
                      workDay.setSaldo(0d);
                      workDay.setWorkedHours(8d);
                      workDay.setUnpaid(0d);
                      fixed = true;
                  }
//                  if (lastCat != null && (lastCat.getCategoryId() == 1)) {
//                      workDay.setWorkedHours(workDay.getWorkedHours() - workDay.getLunch());
//                      workDay.setSaldo(workDay.getSaldo() - workDay.getLunch());
//                  }
              } 
              
              if (!fixed && workDay.getWorkEnd().getDate() != null && workDay.getWorkEnd().getDate().isAfter(globalEnd)) {
                  workDay.setUnpaid(workDay.getUnpaid() + ChronoUnit.SECONDS.between(globalEnd, workDay.getWorkEnd().getDate()) / 3600d);
                  workDay.getWorkEnd().setOriginal(workDay.getWorkEnd().getDate());
                  workDay.getWorkEnd().setDate(globalEnd);
                  workDay.getWorkEnd().setCorrected(true);
              }
              
              sumWork += workDay.getWorkedHours();  

              // generate history
              if (workDay.getDate().isBefore(LocalDate.now().atStartOfDay().toLocalDate())) {

                  Optional<Work> work = works.stream().filter(w -> w.getDate().equals(workDay.getDate())).findFirst();
                  if (work.isPresent() && StringUtils.isEmpty(work.get().getDescription())
                          && work.get().getWorkType() == WorkTypeDTO.NONE && work.get().getWorkType2() == WorkTypeDTO.NONE) {
                      
                      // generate RPA
                      Work worked = work.get();
                      Double sumBase = 0d;
                                            
                      if (workDay.getSick() > 0) {
                          sumBase = round(workDay.getSick());
                          worked.setHours2(sumBase);
                          worked.setWorkType2(WorkTypeDTO.SICKNESS); 
                      } else {
                          if (workDay.getTrip() > 0) {
                              sumBase = round(workDay.getTrip());
                              worked.setHours2(sumBase);
                              worked.setWorkType2(WorkTypeDTO.TRIP);                          
                          }
                      }
                      
                      if (workDay.getWorkedHours() == 0) {
                          worked.setHours(AISService.HOURS_IN_DAY);
                          worked.setWorkType(WorkTypeDTO.HOLIDAY);
                      } else {
                          worked.setHours(AISService.HOURS_IN_DAY - sumBase);
                          worked.setWorkType(WorkTypeDTO.WORK);
                      }
                      
                      workRepository.save(worked);
                  }
              }
              
          }
          
          Work worked = works.stream().filter(w -> w.getDate().equals(date)).findFirst().orElse(null);
          if (worked != null) {
            workDay.setWork(mapper.map(worked, WorkDTO.class));
          }
          
          workMonth.addWorkDaysItem(workDay);
          workMonth.setSumHolidays(sumHolidays);
          workMonth.setSumWorkDays(sumWorkDays);
          workMonth.setSumOnSiteDays(sumWork);
          
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
          
        }

        return workMonth;
    }


    private Double round(Double origin) {        
        double base = Math.floor(origin);
        if (origin - base > .25d) {
            base += .5;
        }
        if (origin - base > .25d) {
            base += .5;
        }        
        return base == 0 ? .5d : base;
    }

    private boolean isDifferent(@Valid OffsetDateTime real, OffsetDateTime computed) {
        if (computed == null) {
            return false;
        }
        return real == null ? true : real.compareTo(computed) != 0;
    }

    private WorkDayTypeDTO getWorkDayType(LocalDate date, CustomerDataDen customerDataDen) {

        if (customerDataDen == null) {
            return getWorkDayType(date);
        }
        
        if (customerDataDen.isJeVikend()) {
            return WorkDayTypeDTO.SATURDAY;
        }
        
        if (customerDataDen.isJeSvatek()) {
            return WorkDayTypeDTO.NATIONAL_HOLIDAY;
        }
        
        return WorkDayTypeDTO.WORKDAY;
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
        workUpdated.setHours(realHours(work.getHoursText()));
        workUpdated.setHours2(realHours(work.getHours2Text()));
        workRepository.save(workUpdated);

    }
    
    public static Double realHours(String value) {

        if (StringUtils.isEmpty(value)) {
            return null;
        }

        try {
            value = value.replaceAll(",", ".");

            if (value.indexOf(":") > 0) {

                int hours = Math.abs(Integer.valueOf(value.substring(0, value.indexOf(":"))));
                int mins = Math.abs(Integer.valueOf(value.substring(value.indexOf(":") + 1)));
                if (value.startsWith("-")) {
                    mins = - mins;
                }

                return hours + mins / 60d;

            } else {

                return Double.valueOf(value);

            }
            
        } catch (Exception e) {
        }

        return null;
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
        

    @SuppressWarnings("deprecation")
    public void toPdf(XSSFWorkbook my_xls_workbook, OutputStream out) throws DocumentException {
      
        XSSFSheet my_worksheet = my_xls_workbook.getSheetAt(0); 
        // To iterate over the rows
        Iterator<Row> rowIterator = my_worksheet.iterator();
        //We will create output PDF document objects at this point
        Document iText_xls_2_pdf = new Document();
        PdfWriter.getInstance(iText_xls_2_pdf, out);
        iText_xls_2_pdf.open();
        //we have two columns in the Excel sheet, so we create a PDF table with two columns
        //Note: There are ways to make this dynamic in nature, if you want to.
        PdfPTable my_table = new PdfPTable(2);
        //We will use the object below to dynamically add new data to the table
        PdfPCell table_cell;
        //Loop through rows.
        while(rowIterator.hasNext()) {
                Row row = rowIterator.next(); 
                Iterator<Cell> cellIterator = row.cellIterator();
                        while(cellIterator.hasNext()) {
                                Cell cell = cellIterator.next(); //Fetch CELL
                                switch(cell.getCellType()) { //Identify CELL type
                                        //you need to add more code here based on
                                        //your requirement / transformations
                                case Cell.CELL_TYPE_STRING:
                                        //Push the data from Excel to PDF Cell
                                         table_cell=new PdfPCell(new Phrase(cell.getStringCellValue()));
                                         //feel free to move the code below to suit to your needs
                                         my_table.addCell(table_cell);
                                        break;
                                }
                                //next line
                        }
    
        }
        //Finally add the table to PDF document
        iText_xls_2_pdf.add(my_table);                       
        iText_xls_2_pdf.close();                
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
        
        XSSFSheet sheet = workbook.createSheet(yearmonth + " " + username);

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
        sheet.setColumnWidth(k++, 200 * PX);

        createSimpleRow(sheet, row++, styles.get(ExcelStyleType.H1), 
                yearmonth, selectedUser.getName(),
                null, null, null, null, "ev.č. " + selectedUser.getCode());               
        
        row++;
        createSimpleRow(sheet, row++, styles.get(ExcelStyleType.TH), 
                "datum", "kategorie", "příchod", "oběd/přestávky", "odchod", "celkem", "výkazy", "", "poznámka (hodiny/zakázka)");

        WorkMonthDTO workMonthDTO = getWorkDays(year, month, username);
        for (WorkDayDTO workDay : workMonthDTO.getWorkDays()) {

            WorkDTO work = workDay.getWork();
            
            createSimpleRow(sheet, row++, styles.get(ExcelStyleType.TD), styles.get(ExcelStyleType.TD_PRICE),    
                    date(workDay.getDate()), getDescription(workDay.getWorkDayType()), hoursOnly(workDay.getWorkStart()), 
                    hours(workDay.getLunch()), 
                    hoursOnly(workDay.getWorkEnd()), hours(workDay.getWorkedHours()),                     
                    work != null ? hours(work.getHours()) : "", work != null ? getDescription(work.getWorkType()) : "", 
                    work != null && !StringUtils.isEmpty(work.getDescription()) ? work.getDescription() : "");

            createSimpleRow(sheet, row++, styles.get(ExcelStyleType.VALUE), styles.get(ExcelStyleType.VALUE_PRICE),    
                    "", "", "", "", "", "", 
                    work != null ? hours(work.getHours2()) : "", work != null ? getDescription(work.getWorkType2()) : "", "");
        }        

        row++;
        
        int startRow = row;
        
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
        double fondOfUser = (selectedUser.getFond() == null ? workMonthDTO.getSumWorkDays() : workMonthDTO.getSumWorkDays() * fond) * HOURS_IN_DAY;
        cell.setCellValue(fondOfUser);

        // Svátky
        index = 0;
        double holidays = 0;
        
        if (workMonthDTO.getSumHolidays() != null && workMonthDTO.getSumHolidays() > 0) {
            rowCell = sheet.createRow(row++);
            cell = rowCell.createCell(index++, CellType.STRING);
            cell.setCellStyle(styles.get(ExcelStyleType.TD));
            cell.setCellValue("Svátky");
            
            cell = rowCell.createCell(index++, CellType.NUMERIC);
            cell.setCellStyle(styles.get(ExcelStyleType.TD_PRICE));
            holidays = workMonthDTO.getSumHolidays() * HOURS_IN_DAY;
            cell.setCellValue(holidays);
        }
        
        index = 0;
        
        rowCell = sheet.createRow(row++);
        cell = rowCell.createCell(index++, CellType.STRING);
        cell.setCellStyle(styles.get(ExcelStyleType.TD_SILVER));
        cell.setCellValue("Celkem");
        
        cell = rowCell.createCell(index++, CellType.NUMERIC);
        cell.setCellStyle(styles.get(ExcelStyleType.TD_SILVER_PRICE));
        cell.setCellValue(fondOfUser + holidays);
        
        
        // Aditus
//        index = 0;
//        rowCell = sheet.createRow(row++);
//        cell = rowCell.createCell(index++, CellType.STRING);
//        cell.setCellStyle(styles.get(ExcelStyleType.TD));
//        cell.setCellValue("ADocházka");
//        
//        cell = rowCell.createCell(index++, CellType.NUMERIC);
//        cell.setCellStyle(styles.get(ExcelStyleType.TD_PRICE));
//        cell.setCellValue(workMonthDTO.getSumOnSiteDays());

        double sumOther = holidays;
        
        for (WorkDTO work : workMonthDTO.getSums()) {
            index = 0;
            if (work.getHours() != null) {
                rowCell = sheet.createRow(row++);
                cell = rowCell.createCell(index++, CellType.STRING);
                cell.setCellStyle(styles.get(ExcelStyleType.TD));
                cell.setCellValue(getDescription(work.getWorkType()));
                
                cell = rowCell.createCell(index++, CellType.NUMERIC);
                cell.setCellStyle(styles.get(ExcelStyleType.TD_PRICE));
                cell.setCellValue(work.getHours());            
                sumOther += work.getHours();
            }
        }

        index = 0;
        rowCell = sheet.createRow(row++);
        cell = rowCell.createCell(index++, CellType.STRING);
        cell.setCellStyle(styles.get(ExcelStyleType.TD_SILVER));
        cell.setCellValue("Celkem");
        
        cell = rowCell.createCell(index++, CellType.NUMERIC);
        cell.setCellStyle(styles.get(ExcelStyleType.TD_SILVER_PRICE));
        cell.setCellValue(sumOther);

        
        rowCell = sheet.createRow(row++);
        rowCell = sheet.createRow(row++);

        rowCell = sheet.createRow(row++);
        cell = rowCell.createCell(0, CellType.STRING);
        cell.setCellStyle(styles.get(ExcelStyleType.TD));
        cell.setCellValue("Uzavřeno dne:");

        cell = rowCell.createCell(5, CellType.STRING);
        cell.setCellStyle(styles.get(ExcelStyleType.TD));
        cell.setCellValue("Schváleno dne:");

        rowCell = sheet.createRow(row++);

        rowCell = sheet.createRow(row++);
        cell = rowCell.createCell(0, CellType.STRING);
        cell.setCellStyle(styles.get(ExcelStyleType.TD));
        cell.setCellValue("Podpis zaměstnance:");

        cell = rowCell.createCell(5, CellType.STRING);
        cell.setCellStyle(styles.get(ExcelStyleType.TD));
        cell.setCellValue("Podpis vedoucího:");
        
        
        sheet.createFreezePane(2, 3);
        sheet.setRepeatingRows(CellRangeAddress.valueOf("A1:H3"));

        createFooter(sheet, false);
        sheet.setFitToPage(true);

        String password= "Veritas03a";
        sheet.protectSheet(password);
        sheet.enableLocking();
                
        CTSheetProtection sheetProtection = sheet.getCTWorksheet().getSheetProtection();
        sheetProtection.setSelectLockedCells(true); 
        sheetProtection.setSelectUnlockedCells(false); 
        sheetProtection.setFormatCells(true); 
        sheetProtection.setFormatColumns(true); 
        sheetProtection.setFormatRows(true); 
        sheetProtection.setInsertColumns(true); 
        sheetProtection.setInsertRows(true); 
        sheetProtection.setInsertHyperlinks(true); 
        sheetProtection.setDeleteColumns(true); 
        sheetProtection.setDeleteRows(true); 
        sheetProtection.setSort(false); 
        sheetProtection.setAutoFilter(false); 
        sheetProtection.setPivotTables(true); 
        sheetProtection.setObjects(true); 
        sheetProtection.setScenarios(true);

        if (out != null) {
            workbook.write(out);
//            try {
//                toPdf(workbook, out);
//            } catch (DocumentException e) {
//            }
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

        XSSFCellStyle styleTDSilver = workbook.createCellStyle();
        styleTDSilver.setFont(fontValue);
        styleTDSilver.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styleTDSilver.setFillForegroundColor(COLOR_SILVER);
        styleTDSilver.setWrapText(false);
//        styleTD.setBorderBottom(BorderStyle.THIN);
//        styleTD.setBottomBorderColor(COLOR_SILVER);
        styles.put(ExcelStyleType.TD_SILVER, styleTDSilver);

        
        XSSFCellStyle styleTDSilverPrice = workbook.createCellStyle();
        styleTDSilverPrice.setFont(fontValue);
        styleTDSilverPrice.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styleTDSilverPrice.setFillForegroundColor(COLOR_SILVER);
        styleTDSilverPrice.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        styleTDSilverPrice.setAlignment(HorizontalAlignment.RIGHT);
//        styleTDPrice.setBorderBottom(BorderStyle.THIN);
//        styleTDPrice.setBottomBorderColor(COLOR_SILVER);
        styles.put(ExcelStyleType.TD_SILVER_PRICE, styleTDSilverPrice);

        
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
