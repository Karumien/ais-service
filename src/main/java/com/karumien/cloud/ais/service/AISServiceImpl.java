/*
 * Copyright (c) 2019-2029 Karumien s.r.o.
 *
 * Karumien s.r.o. is not responsible for defects arising from 
 * unauthorized changes to the source code.
 */
package com.karumien.cloud.ais.service;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
			workDay.setWorks(works.stream().filter(w -> w.getDate().equals(date)).map(w -> mapper.map(w, WorkDTO.class))
					.collect(Collectors.toList()));
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
				workDay.getWorkEnd().setDate(OffsetDateTime.of(LocalDateTime.of(year, month, day, 16, 00),
						OffsetDateTime.now().getOffset()));
				workDay.getWorkEnd().setCorrected(true);
				workedMinutes += lastIn.getDate().until(workDay.getWorkEnd().getDate(), ChronoUnit.MINUTES);
			}
			if (lunchStart != null && lunchEnd == null) {
				workDay.setLunchEnd(new WorkHourDTO());
				workDay.getLunchEnd().setDate(lunchStart.getDate().plusMinutes(30));
				workDay.getLunchEnd().setCorrected(true);
				workedMinutes -= 30;
			}
			if (lunchStart == null && lunchEnd == null && workStart != null) {
				workDay.setLunchStart(new WorkHourDTO());
				workDay.getLunchStart().setDate(OffsetDateTime.of(LocalDateTime.of(year, month, day, 11, 00),
						OffsetDateTime.now().getOffset()));
				workDay.getLunchStart().setCorrected(true);
				workDay.setLunchEnd(new WorkHourDTO());
				workDay.getLunchEnd().setDate(workDay.getLunchStart().getDate().plusMinutes(30));
				workDay.getLunchEnd().setCorrected(true);
				workedMinutes -= 30;
			}
			OffsetDateTime realStart = OffsetDateTime.of(LocalDateTime.of(year, month, day, 6, 30), OffsetDateTime.now().getOffset());
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

		UserInfo currentUser = userInfoRepository.findByUsername(username)
				.orElseThrow(() -> new NoDataFoundException("NO.USER", "No User for USERNAME = " + username));

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
	public Long setWork(@NotNull @Valid LocalDate date, @NotNull @Valid String username, @Valid String hours,
			@Valid Long id, @Valid String workType) {

		Work work = null;
		
		Double workHours = null;
		try {
			workHours = (hours == null || hours.trim().length() == 0) ? null : Double.valueOf(hours.replace(',', '.').trim());
		} catch (Exception e) {
		}

		if (id != null && (workHours == null || workType == null)) {
			workRepository.deleteById(id);
			return null;
		}
		
		if (workHours == null || workType == null) {
			return null;
		}
			
		if (id != null) {
			work = workRepository.findById(id).orElse(null);
		} 
		
		if (work == null) {
			work = new Work();
			work.setDate(date);
			work.setUsername(username);
			work.setWorkDayType(getWorkDayType(date));
		}

		work.setWorkType(WorkTypeDTO.fromValue(workType));
		work.setHours(BigDecimal.valueOf(workHours).setScale(2, RoundingMode.FLOOR).doubleValue());
		return workRepository.save(work).getId();
	}
}
