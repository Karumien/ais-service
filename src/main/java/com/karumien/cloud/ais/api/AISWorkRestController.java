package com.karumien.cloud.ais.api;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.karumien.cloud.ais.api.entity.Work;
import com.karumien.cloud.ais.api.handler.WorkApi;
import com.karumien.cloud.ais.api.model.UserInfoDTO;
import com.karumien.cloud.ais.api.model.WorkDTO;
import com.karumien.cloud.ais.api.model.WorkDayDTO;
import com.karumien.cloud.ais.api.model.WorkDayTypeDTO;
import com.karumien.cloud.ais.api.model.WorkHourDTO;
import com.karumien.cloud.ais.api.model.WorkMonthDTO;
import com.karumien.cloud.ais.api.model.WorkTypeDTO;
import com.karumien.cloud.ais.service.AISService;

/**
 * REST API for AIS Services.
 *
 * @author <a href="miroslav.svoboda@karumien.com">Miroslav Svoboda</a>
 * @since 1.0, 15. 4. 2019 18:54:23
 */
@RestController
@RequestMapping(path = "/api")
public class AISWorkRestController implements WorkApi {
	
	@Autowired
	private ModelMapper mapper;
	
	@Autowired
	private AISService aisService;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ResponseEntity<WorkMonthDTO> getWorkDays(@NotNull @Valid String username, @Valid Integer year,
			@Valid Integer month) {
		return new ResponseEntity<>(
				mapper.map(aisService.getWorkDays(year, month, username), WorkMonthDTO.class), HttpStatus.OK);
	}
		
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ResponseEntity<List<UserInfoDTO>> getWorkUsers(@Valid String username) {
		return new ResponseEntity<>(aisService.getWorkUsers(username), HttpStatus.OK);
	}
	
	/**
	 * HTML formated Users on site.
	 * 
	 * @deprecated will be replaced by UI application
	 * @return HTML table of Users on site
	 */
	@RequestMapping(value = "/work/html", produces = { "text/html" }, method = RequestMethod.GET)
	@Deprecated
	public String getUserMonthHTML(@NotNull @Valid @RequestParam(value = "role", required = true) String role,
			@Valid @RequestParam(value = "username", required = false) String username,
			@Valid @RequestParam(value = "month", required = false) Integer month, 
			@Valid @RequestParam(value = "year", required = false) Integer year) {
		
		if (year == null) {
			year = LocalDate.now().getYear();
		}

		if (month == null) {
			month = LocalDate.now().getMonthValue();
		}
		
		if (username == null) {
			username = role;
		}		
		
		StringBuilder sb = new StringBuilder("<table cellspacing=\"5\" class=\"aditus\"><form action=\"/api/work/html\" method=\"get\">");
		sb.append("<tr><td colspan=\"7\"><select name=\"month\" onchange=\"this.form.submit()\">");
		
		List<String> months = Arrays.asList("leden", "únor", "březen", "duben", "květen", "červen", 
				"červenec", "srpen", "září", "říjen", "listopad", "prosinec");
		for (int i = 0; i < 12; i++) {
			sb.append("<option value=\"").append(i+1).append("\"").append(month.equals(i+1) ? " selected" : "");
			sb.append(">").append(months.get(i)).append("</option>");
		}
		sb.append("</select><select><option selected>2019</select><input type=\"hidden\" name=\"role\" value=\"").append(role).append("\"></td>");
		
		sb.append("<td align=\"right\"><select name=\"username\" onchange=\"this.form.submit()\">");
		for (UserInfoDTO user : aisService.getWorkUsers(role)) {
			sb.append("<option value=\"").append(user.getUsername()).append("\"").append(username.equals(user.getUsername()) ? " selected" : "");
			sb.append(">").append(user.getName()).append("</option>");			
		}
		sb.append("</select></td></tr>");				
		
		sb.append("<tr>"
			+ "<td class=\"i24_tableHead menuline\" align=\"right\">Datum</td>"
			+ "<td class=\"i24_tableHead menuline\" align=\"right\">Kategorie</td>"
			+ "<td class=\"i24_tableHead menuline\">Příchod</td>"
			+ "<td class=\"i24_tableHead menuline\" align=\"center\">Oběd od-do</td>"
			+ "<td class=\"i24_tableHead menuline\">Odchod</td>"
			+ "<td class=\"i24_tableHead menuline\" align=\"right\">Celkem</td><td>&nbsp;</td>"
			+ "<td class=\"i24_tableHead menuline\">Výkazy</td></tr>");
		
		WorkMonthDTO workMonthDTO = aisService.getWorkDays(year, month, username);
		for (WorkDayDTO workDay : workMonthDTO.getWorkDays()) {
			
			sb.append("<tr>");
			sb.append("<td class=\"i24_tableItem\"><i>").append(workDay.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))).append("</i></td>");
			sb.append("<td class=\"i24_tableItem\">").append(getDescription(workDay.getWorkDayType())).append("</td>");
			sb.append("<td class=\"i24_tableItem\"><b>").append(hoursOnly(workDay.getWorkStart())).append("</b></td>");
			sb.append("<td class=\"i24_tableItem\" align=\"center\">")
				.append(hoursOnly(workDay.getLunchStart())).append(workDay.getLunchStart() != null ? " - " : "").append(hoursOnly(workDay.getLunchEnd())).append("</td>");
			sb.append("<td class=\"i24_tableItem\"><b>").append(hoursOnly(workDay.getWorkEnd())).append("</b></td>");
			sb.append("<td class=\"i24_tableItem\" align=\"right\"><b>").append(hours(workDay.getWorkedHours())).append("</b></td>");
			
			if (workDay.getDate().getDayOfWeek() != DayOfWeek.SATURDAY
					&& workDay.getDate().getDayOfWeek() != DayOfWeek.SUNDAY 
					&& workDay.getWorkDayType() != WorkDayTypeDTO.NATIONAL_HOLIDAY) {
				for (int a = 0; a < 2; a++) {

					WorkDTO work = null;
					if (workDay.getWorks() == null || workDay.getWorks().size() <= a) {
						work = null;
					} else {
						work = workDay.getWorks().get(a);
					}
					
					sb.append("<td class=\"i24_tableItem\"><input type=\"text\" style=\"width: 35px; margin-left:10px\" value=\"").append(work != null ? hours(work.getHours()) : "").append("\"><select>");
					for (WorkTypeDTO type: WorkTypeDTO.values()) {
						sb.append("<option value=\"").append(type.name()).append("\"").append(work != null && work.getWorkType() == type ? " selected" : "");
						sb.append(">").append(getDescription(type)).append("</option>");
					}
					sb.append("</select></td>");
				}
			}
						
			sb.append("</tr>");
			
			if (workDay.getDate().getDayOfWeek() == DayOfWeek.SUNDAY) {
				sb.append("<tr><td colspan=\"8\"><hr/></td></tr>");
			}
		}
		sb.append("<tr><td colspan=\"8\" align=\"right\"><input type=\"submit\" value=\"Uložit změny\"></td></tr>");
		sb.append("</form></table>");

		
		StringBuilder sb1 = new StringBuilder("<table cellspacing=\"5\" class=\"aditus\"><tr>");
		StringBuilder sb2 = new StringBuilder("<tr>");


		sb1.append("<td class=\"i24_tableItem\"><i>").append("Fond").append("</i></td>");
		sb2.append("<td class=\"i24_tableItem\"><b>").append(days(workMonthDTO.getSumWorkDays())).append("</b></td>");
		
		sb1.append("<td class=\"i24_tableItem\"><i>").append("Svátky").append("</i></td>");
		sb2.append("<td class=\"i24_tableItem\"><b>").append(days(workMonthDTO.getSumHolidays())).append("</b></td>");

		sb1.append("<td class=\"i24_tableItem\" style=\"#888888\"><i>").append("Aditus").append("</i></td>");
		sb2.append("<td class=\"i24_tableItem\" style=\"#888888\"><b>").append(days(workMonthDTO.getSumOnSiteDays())).append("</b></td>");

		for (WorkDTO work : workMonthDTO.getSums()) {
			sb1.append("<td class=\"i24_tableItem\"><i>").append(getDescription(work.getWorkType())).append("</i></td>");
			sb2.append("<td class=\"i24_tableItem\"><b>")
					.append(days(work.getHours() == null ? null : work.getHours() / AISService.HOURS_IN_DAY)).append("</b></td>");
		}
		
		sb2.append("</tr>");
		sb1.append(sb2);
		sb1.append("</tr></table>");
		sb.append(sb1);
		return sb.toString();
	}

	private String getDescription(@Valid WorkDayTypeDTO workDayType) {
		switch (workDayType) {
		case NATIONAL_HOLIDAY:
			return "<b>Státní svátek</b>";			
		case WORKDAY:
			return "Pracovní den";
		default:
			return "";
		}
	}
	
	private String getDescription(@Valid WorkTypeDTO workType) {
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
			return "Neschopenka";
		case TIMEOFF:
			return "Neplacené volno";
		default:
			return "";
		}
	}

	private String hours(Double workedHours) {
		if (workedHours == null) {
			return "";
		}
		NumberFormat formatter = new DecimalFormat("#0.00");     
		return formatter.format(workedHours);
	}
	
	private String days(Number workedHours) {
		if (workedHours == null) {
			return "";
		}
		NumberFormat formatter = new DecimalFormat("#0.00");     
		return formatter.format(workedHours);
	}

	private String hoursOnly(@Valid WorkHourDTO work) {
		if (work == null || work.getDate() == null) {
			return "";
		}
		
		return "<span style=\"color:" + (work.isCorrected() ? "#888888":"#000") +"\">" +
				work.getDate().format(DateTimeFormatter.ofPattern("HH:mm")) + "</span>";
	}
	
	@Override
	public ResponseEntity<Long> setWork(@NotNull @Valid LocalDate date, @NotNull @Valid String username,
			@Valid String hours, @Valid Long id, @Valid String workType) {
		return new ResponseEntity<>(aisService.setWork(date, username, hours, id, workType), HttpStatus.OK);
	}	
}
