package com.karumien.cloud.ais.api;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.karumien.client.adochazka.schemas.Uzivatel;
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
    
    private static final String ATTACHMENT_FILENAME = "attachment; filename=";

    private static final String CONTENT_DISPOSITION = "Content-disposition";
//    private static final String CONTENT_TYPE = "content-type";

    /** MediaType Application Excel Openformat */
    private static final String APPLICATION_EXCEL_VALUE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    
    @Autowired
    private ModelMapper mapper;
    
    @Autowired
    private AISService aisService;
    
    @Value(value = "${jsp.redirect:false}")
    private Boolean redirect;

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
     * GET /work/export/xls : Generate export workdays
     *
     * @param response
     *            {@link HttpServletResponse}
     * @throws IOException
     *             on IO error
     */
    @RequestMapping(value = "/work/export", produces = { APPLICATION_EXCEL_VALUE }, method = RequestMethod.POST)
    public void exportWorkDays(@NotNull @Valid @RequestParam(value = "role", required = true) String role,
            @Valid @RequestParam(value = "username", required = false) String username,
            @Valid @RequestParam(value = "month", required = false) Integer month, 
            @Valid @RequestParam(value = "year", required = false) Integer year,
            HttpServletResponse response) throws IOException {

        if (year == null) {
            year = LocalDate.now().getYear();
        }

        if (month == null) {
            month = LocalDate.now().getMonthValue();
        }

        if (username == null) {
            username = role;
        }        

        String yearmonth = year + "." + (month < 10 ? "0" : "") + month;
        response.setHeader(CONTENT_DISPOSITION, ATTACHMENT_FILENAME + yearmonth + "-" + username + ".xlsx");
        aisService.exportWorkDays(year, month, username, response.getOutputStream());
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

        LocalDate actualMonthDay = LocalDate.now();
        LocalDate selectedMonthDay = LocalDate.of(year, month, 1);
        boolean currentMonth = (actualMonthDay.getYear() == year && actualMonthDay.getMonthValue() == month);

        StringBuilder sb = new StringBuilder("<script type=\"text/javascript\">"
        + "function updateWork(form, username) {"
        + "  var xhttp = new XMLHttpRequest();"
        + "  xhttp.open(\"POST\", \""+ (Boolean.TRUE.equals(redirect) ? "" : "http://192.168.2.222:2222") + "/api/work/update?username=" 
        + "\"+username, true);"
        + "  xhttp.setRequestHeader(\"Content-type\", \"application/json\");"
        + "  if (form.hours.value && form.workType.value != 'NONE' && form.hours2.value && form.workType2.value != 'NONE'"
        + "  ||  !form.hours.value && form.workType.value == 'NONE' && form.hours2.value && form.workType2.value != 'NONE'"
        + "  ||  form.hours.value && form.workType.value != 'NONE' && !form.hours2.value && form.workType2.value == 'NONE'"
        + "  ||  !form.hours.value && form.workType.value == 'NONE' && !form.hours2.value && form.workType2.value == 'NONE'"
        + "  ||  form.description.value != form.originalDescription.value"
        + ") {"
        + "  xhttp.send('{ \"id\": ' + form.id.value + '," 
        + "        \"hoursText\": ' + (!form.hours.value ? null : '\"' + form.hours.value + '\"') + ',"
        + "        \"hours2Text\": ' + (!form.hours2.value ? null : '\"' + form.hours2.value + '\"') + ',"
        + "        \"workType\": \"' + form.workType.value + '\","
        + "        \"workType2\": \"' + form.workType2.value + '\","
        + "        \"description\": \"' + form.description.value + '\" }');"
        + "  }}</script>");

        sb.append("<table cellspacing=\"5\" class=\"aditus\"><form action=\""+ 
                (Boolean.TRUE.equals(redirect) ? "/api/work/html" : "/ais.jsp" ) + "\" method=\"get\">");
        sb.append("<tr><td colspan=\"8\"><select name=\"month\" class=\"unvisiblelines\" onchange=\"this.form.submit()\">");
        
        List<String> months = Arrays.asList("leden", "únor", "březen", "duben", "květen", "červen", 
                "červenec", "srpen", "září", "říjen", "listopad", "prosinec");
        for (int i = 4; i < 12; i++) {
            sb.append("<option value=\"").append(i+1).append("\"").append(month.equals(i+1) ? " selected" : "");
            sb.append(">").append(months.get(i)).append("</option>");
        }
        sb.append("</select><select class=\"unvisiblelines\"><option selected>2019</select><input type=\"hidden\" name=\"role\" value=\"").append(role).append("\">");
        
        UserInfoDTO selectedUser = mapper.map(aisService.getUser(username), UserInfoDTO.class);
        UserInfoDTO roleUser = mapper.map(aisService.getUser(role), UserInfoDTO.class);
        if (roleUser == null) {
            roleUser = selectedUser;
        }

        Uzivatel uzivatel = aisService.getUzivatel(username);
        
        sb.append("</td>");
        sb.append("<td align=\"right\"><select class=\"unvisiblelines\" name=\"username\" onchange=\"this.form.submit()\">");
                            
        for (UserInfoDTO user : aisService.getWorkUsers(role)) {
            sb.append("<option value=\"").append(user.getUsername()).append("\"").append(username.equals(user.getUsername()) ? " selected" : "");
            sb.append(">").append(user.getName()).append("</option>");            
        }
        
        sb.append("</select></td><td></td><td align=\"right\">");
        if ((Boolean.TRUE.equals(roleUser.isRoleAdmin()) || Boolean.TRUE.equals(roleUser.isRoleHip())) && !currentMonth && selectedMonthDay.isBefore(actualMonthDay)) {
            sb.append("<a href=\"#\" class=\"buttonSubmit\" title=\"Schválit vybraný měsíc dané osobě\">&nbsp; Schválit</a>");
        }
        sb.append("&nbsp;<a href=\"/works.do?action=list&object=native_works&clear=1\" target=\"_parent\" class=\"buttonSubmit\">&nbsp; Výkazy zakázky</a></td></tr></form>");                
        
        sb.append("<form id=\"exportForm\" action=\"" + (Boolean.TRUE.equals(redirect) ? "" : "http://192.168.2.222:2222") + "/api/work/export?username=" 
                +username+"&role="+role+"&year="+year+"&month="+month + "\" method=\"post\">");
        sb.append("<tr>");
        sb.append("<td class=\"i24_tableHead menuline\" align=\"right\">Datum</td>"
            + "<td class=\"i24_tableHead menuline\" align=\"right\">Kategorie</td>"
            + "<td class=\"i24_tableHead menuline\">Příchod</td>"
            + "<td class=\"i24_tableHead menuline\" align=\"center\">Oběd/přest.</td>"
            + "<td class=\"i24_tableHead menuline\">Odchod</td>"
//            + "<td class=\"i24_tableHead menuline\" align=\"right\">Služebně</td>"
//            + "<td class=\"i24_tableHead menuline\" align=\"right\">Nemoc</td>"
            + "<td class=\"i24_tableHead menuline\" align=\"left\">Celkem</td>"
            + "<td class=\"i24_tableHead menuline\" align=\"right\">Saldo</td>"
            + "<td>&nbsp;</td>"
            + "<td class=\"i24_tableHead menuline\" style=\"text-align: right\">Výkazy (").append(username).append(")");

        //if (roleUser.isRoleAdmin()) {
            sb.append("<a href=\"#\" onclick=\"document.getElementById('exportForm').submit();\">");
            sb.append("<img onclick=\"this.form.submit();\" src=\"/img/printer.gif\" style=\"position: relative; top: 4px; margin-left: 6px; width: 15px; height: 16px;\" border=\"0\"/></a>");
        //}
        sb.append("</td><td></td><td class=\"i24_tableHead menuline\">&nbsp; Poznámka (hodiny/zakázka)</td></tr></form>");

        
        double fond = selectedUser.getFond() != null ? selectedUser.getFond() / 100d : 1d;
        double saldo = 0;
        
        
        WorkMonthDTO workMonthDTO = aisService.getWorkDays(year, month, username);
        for (WorkDayDTO workDay : workMonthDTO.getWorkDays()) {
            
            WorkDTO work = workDay.getWork();

            sb.append("<tr>");
            
            if (work != null) {
                sb.append("<form name=\"form"+ work.getId() +"\">");
            }
            
            sb.append("<td class=\"i24_tableItem\"><i>").append(aisService.date(workDay.getDate())).append("</i></td>");
            sb.append("<td class=\"i24_tableItem\">").append(getDescription(workDay.getWorkDayType())).append("</td>");
            sb.append("<td class=\"i24_tableItem\"><b>").append(hoursOnly(workDay.getWorkStart())).append("</b></td>");
           
            if (workDay.getWorkDayType() == WorkDayTypeDTO.WORKDAY && actualMonthDay.isAfter(workDay.getDate()) || actualMonthDay.equals(workDay.getDate())) {
              sb.append("<td class=\"i24_tableItem\" align=\"center\">")
                .append(aisService.hours(workDay.getLunch())).append("</td>");
            }
            
            sb.append("<td class=\"i24_tableItem\"><b>").append(hoursOnly(workDay.getWorkEnd())).append("</b></td>");
            
            String ro = " disabled=\"disabled\" ";
            if (Boolean.TRUE.equals(roleUser.isRoleHip()) || Boolean.TRUE.equals(roleUser.isRoleAdmin())) {
                ro = "";
            }
            
            if (workDay.getDate().getDayOfWeek() != DayOfWeek.SATURDAY
                && workDay.getDate().getDayOfWeek() != DayOfWeek.SUNDAY 
                && workDay.getWorkDayType() != WorkDayTypeDTO.NATIONAL_HOLIDAY) {
                                
                if (workDay.getWork() == null) {
                    continue;
                }
                                
                if (workDay.getWorkDayType() == WorkDayTypeDTO.WORKDAY && actualMonthDay.isAfter(workDay.getDate()) || actualMonthDay.equals(workDay.getDate())) {
                    
//                    sb.append("<td class=\"i24_tableItem\" align=\"right\">").append(
//                            aisService.hours(workDay.getTrip(), false)).append("</td>");
//                    sb.append("<td class=\"i24_tableItem\" align=\"right\">").append(
//                            aisService.hours(workDay.getSick(), false)).append("</td>");

                    String adv = "";
                    if (workDay.getTrip() != null && workDay.getTrip() > 0) {
                        adv += "Služební cesta:  " + aisService.hours(workDay.getTrip(), false) + "\n";
                    }
                    if (workDay.getSick() != null && workDay.getSick() > 0) {
                        adv += "Lékař/Nemoc :  " + aisService.hours(workDay.getSick(), false) + "\n";
                    }
                    
                    
                    sb.append("<td class=\"i24_tableItem\" align=\"left\"><div "
                            + (adv.length() > 0 ? "title =\"" + adv + "\"" : "" ) + 
                            "><b>").append(aisService.hours(workDay.getWorkedHours())).append("</b>"
                            + (adv.length() > 0 ? "<span class=\"i24_tableHead menuline\"> (?)</span>" : "") 
                            + "</div></td>");
                    sb.append("<td class=\"i24_tableItem\" align=\"right\">").append(work != null ? saldo(workDay.getSaldo()) : "").append("<td>");
                    
                    saldo += workDay.getSaldo() != null ? workDay.getSaldo() : 0;

                } else {

//                    sb.append("<td class=\"i24_tableItem\" align=\"right\">").append("</td>");
//                    sb.append("<td class=\"i24_tableItem\" align=\"right\">").append("</td>");
    
                    sb.append("<td class=\"i24_tableItem\" align=\"right\">").append("</td>");
                    sb.append("<td class=\"i24_tableItem\" align=\"right\">").append("<td>");
                }
                
                sb.append("<td class=\"i24_tableItem\"><input type=\"hidden\" name=\"id\" value=\""+ work.getId() +"\">"
                        + "<input class=\"unvisiblelines\" onChange=\"updateWork(this.form, '"+username+"')\" type=\"text\" name=\"hours\" style=\"width: 35px; margin-left:10px\" value=\"")
                    .append(work != null ? aisService.hours(work.getHours()) : "")
                    .append("\"><select class=\"unvisiblelines\" name=\"workType\" onChange=\"updateWork(this.form, '"+username+"')\">");
                for (WorkTypeDTO type: WorkTypeDTO.values()) {
                    sb.append("<option value=\"").append(type.name()).append("\"").append(work != null && work.getWorkType() == type ? " selected" : "");
                    sb.append(">").append(aisService.getDescription(type)).append("</option>");
                }
                sb.append("</select></td>");

                sb.append("<td class=\"i24_tableItem\"><input class=\"unvisiblelines\" onChange=\"updateWork(this.form, '"+username+"')\" name=\"hours2\" type=\"text\" style=\"width: 35px; margin-left:10px\" value=\"")
                    .append(work != null ? aisService.hours(work.getHours2()) : "")
                    .append("\"><select class=\"unvisiblelines\" name=\"workType2\" onChange=\"updateWork(this.form, '"+username+"')\">");
                for (WorkTypeDTO type: WorkTypeDTO.values()) {
                    sb.append("<option value=\"").append(type.name()).append("\"").append(work != null && work.getWorkType2() == type ? " selected" : "");
                    sb.append(">").append(aisService.getDescription(type)).append("</option>");
                }
                sb.append("</select></td>");

                sb.append("<td class=\"i24_tableItem\"><input type=\"hidden\" name=\"originalDescription\" value=\"")
                    .append(work != null && work.getDescription() != null ? work.getDescription() : "")
                    .append("\"><input class=\"unvisiblelines\" onChange=\"updateWork(this.form, '"+username+"')\" name=\"description\" type=\"text\" style=\"width: 350px; margin-left:10px\" value=\"")
                    .append(work != null && work.getDescription() != null ? work.getDescription() : "")
                    .append("\"></td>");
            }
                        
            if (work != null) {
                sb.append("</form>");
            }

            sb.append("</tr>");
            
            if (workDay.getDate().getDayOfWeek() == DayOfWeek.SUNDAY && ! workDay.getDate().isEqual(workDay.getDate().with(TemporalAdjusters.lastDayOfMonth()))) {
                sb.append("<tr><td colspan=\"11\"><hr/></td></tr>");
            }
        }
        sb.append("</table>");

        
        StringBuilder sb1 = new StringBuilder("<table cellspacing=\"0\" cellpadding=\"5\" class=\"aditus\"><tr><td colspan=\"5\"><hr/></td></tr><tr>");
        StringBuilder sb2 = new StringBuilder("<tr>");

        sb1.append("<td class=\"i24_tableItem\"><i>").append("Svátky").append("</i></td>");
        sb2.append("<td class=\"i24_tableItem\"><b>").append(aisService.days(workMonthDTO.getSumHolidays())).append("</b></td>");

        sb1.append("<td class=\"i24_tableItem\"><i><b>").append("Fond").append("</b></i></td>");
        sb2.append("<td class=\"i24_tableItem\"><b>").append(
                selectedUser.getFond() == null ? aisService.days(workMonthDTO.getSumWorkDays()) : 
                    aisService.days(workMonthDTO.getSumWorkDays() * fond) + "</b> / " + aisService.days(workMonthDTO.getSumWorkDays())
        ).append("</b></td>");
        

        if (uzivatel != null) {
            sb1.append("<td class=\"i24_tableItem\" style=\"#888888\"><i title=\"Saldo k dnešnímu dni\">").append("ADocházka (?)").append("</i></td>");        
            sb2.append("<td class=\"i24_tableItem\"><b>").append(aisService.hours(workMonthDTO.getSumOnSiteDays())).append("</b> (" + 
                    saldo( saldo )).append(")</td>");
        }
        
        double worked = 0;
        
        // work types
        for (WorkDTO work : workMonthDTO.getSums()) {
            if (aisService.isWorkingType(work.getWorkType())) {
                worked += work.getHours() == null ? 0 : work.getHours() / AISService.HOURS_IN_DAY;
                sb1.append("<td class=\"i24_tableItem\" style=\"background-color: #7FDBFF\"><i>").append(aisService.getDescription(work.getWorkType())).append("</i></td>");
                sb2.append("<td class=\"i24_tableItem\" style=\"background-color: #7FDBFF\"><b>")
                        .append(aisService.days(work.getHours() == null ? null : work.getHours() / AISService.HOURS_IN_DAY)).append("</b></td>");
            }
        }
        
        sb1.append("<td class=\"i24_tableItem\"><i><b>").append("Odpracováno").append("</b></i></td>");
        sb2.append("<td class=\"i24_tableItem\"><b>").append(aisService.days(worked)).append("</b></td>");
                
        // non-work types
        for (WorkDTO work : workMonthDTO.getSums()) {
            if (!aisService.isWorkingType(work.getWorkType())) {
                sb1.append("<td class=\"i24_tableItem\"><i>").append(aisService.getDescription(work.getWorkType())).append("</i></td>");
                sb2.append("<td class=\"i24_tableItem\"><b>")
                        .append(aisService.days(work.getHours() == null ? null : work.getHours() / AISService.HOURS_IN_DAY)).append("</b></td>");
            }
        }

        if (uzivatel != null) {
            sb1.append("<td class=\"i24_tableItem\"><i>").append("Saldo").append("</i></td>");
            sb2.append("<td class=\"i24_tableItem\"><b>").append(saldo(workMonthDTO.getSumOnSiteDays() - worked * AISService.HOURS_IN_DAY)).append("</b></td>");
        }

        sb2.append("</tr>");
        sb1.append(sb2);
        sb1.append("</tr></table>");
        sb.append(sb1);
        return sb.toString();
    }
    
    private String saldo(Double value) {
        if (value == null) {
            return "";
        }
        
        return "<span style=\"color:" + (value < 0 ? "#FF4136" : "#2ECC40") + "\">" + aisService.formatAsTime(value) + "</span>";
    }

    private String hoursOnly(@Valid WorkHourDTO work) {
        if (work == null || work.getDate() == null) {
            return "";
        }
        return "<span style=\"color:" + (work.isCorrected() ? "#888888":"#000") +"\">" 
           + aisService.hoursOnly(work) + "</span>";
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
    
    /**
     * {@inheritDoc}
     */
    @Override
    @CrossOrigin(origins = "*")
    public ResponseEntity<Void> setWork(@Valid WorkDTO work, @NotNull @Valid String username) {
        aisService.setWork(work, username);
        return new ResponseEntity<>(HttpStatus.OK);
    }
    
}
