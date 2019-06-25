package com.karumien.cloud.ais.api;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.karumien.cloud.ais.api.handler.PassApi;
import com.karumien.cloud.ais.api.model.PassDTO;
import com.karumien.cloud.ais.service.AISService;

/**
 * REST API for AIS Services.
 *
 * @author <a href="miroslav.svoboda@karumien.com">Miroslav Svoboda</a>
 * @since 1.0, 15. 4. 2019 18:54:23
 */
@RestController
@RequestMapping(path = "/api")
public class AISPassRestController implements PassApi {

    @Autowired
    private ModelMapper mapper;
    
    @Autowired
    private AISService aisService;

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<List<PassDTO>> getPass(@Valid String username) {
        return new ResponseEntity<>(
                aisService.getPass(username).get()
                    .map(pass -> mapper.map(pass, PassDTO.class)).collect(Collectors.toList()), HttpStatus.OK);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<List<PassDTO>> getUsersOnsite() {
        return new ResponseEntity<>(        
            aisService.getPassOnsite().stream()
                .map(pass -> mapper.map(pass, PassDTO.class)).collect(Collectors.toList()), HttpStatus.OK);
    }

    /**
     * HTML formated Users on site.
     * 
     * @deprecated will be replaced by UI application
     * @return HTML table of Users on site
     */
    @RequestMapping(value = "/pass/onsite/html", produces = { "text/html" }, method = RequestMethod.GET)
    @Deprecated
    public String getUsersOnsiteHTML() {
        StringBuilder sb = new StringBuilder("<table cellspacing=\"5\" class=\"aditus\">");
        
        List<PassDTO> lines = getUsersOnsite().getBody();
        
        boolean hrShowed = false;
        
        if (lines.isEmpty()) {
            sb.append("<tr><td><p>Nenalezen žádný záznam.</p></td></tr>");
        } else {
            sb.append("<tr><td class=\"i24_tableHead menuline\">Čas</td>"
                    + "<td class=\"i24_tableHead menuline\">Středisko</td>"
                    + "<td class=\"i24_tableHead menuline\">Osoba</td>"
                    + "<td class=\"i24_tableHead menuline\">Událost</td></tr>");
            
            for (PassDTO p : lines) {
                
                if (p.getCategoryId() != 1 && !hrShowed) {
                    hrShowed = true;
                    sb.append("<tr><td colspan=\"4\"><hr/></td></tr>");
                }
                
                sb.append("<tr>")
                    .append("</td><td class=\"i24_tableItem\">").append(p.getDate().format(DateTimeFormatter.ofPattern("HH:mm")))
                    .append("</td><td class=\"i24_tableItem\">").append(p.getPerson() == null ? "" : p.getPerson().getDepartment())
                    .append("</td><td class=\"i24_tableItem\">").append(p.getCategoryId() == 1 && p.getPerson() != null ? "<b>" : "")
                    .append(p.getPerson() == null ? "<i>&lt;neznámý&gt;</i>" : p.getPerson().getName())
                    .append(p.getCategoryId() == 1 && p.getPerson() != null ? "</b>" : "")
                    .append("</td><td class=\"i24_tableItem\">").append(p.getCategory())
                    .append("</td></tr>");
            }
        }
        sb.append("</table>");
        return sb.toString();
    }
    
}
