package com.karumien.cloud.ais.api;

import java.util.List;
import java.util.stream.Collectors;

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
public class AISRestController implements PassApi {

	@Autowired
	private ModelMapper mapper;
	
	@Autowired
	private AISService aisService;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ResponseEntity<List<PassDTO>> getPass(Integer usercode) {
		return new ResponseEntity<>(
				aisService.getPass(usercode).get()
					.map(pass -> mapper.map(pass, PassDTO.class)).collect(Collectors.toList()), HttpStatus.OK);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ResponseEntity<PassDTO> getPassById(Integer passId) {
		return new ResponseEntity<>(
			mapper.map(aisService.getPassById(passId), PassDTO.class), HttpStatus.OK);
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
		
		if (lines.isEmpty()) {
			sb.append("<tr><td><p>Nenalezen žádný záznam.</p></td></tr>");
		} else {
			sb.append("<tr><td class=\"i24_tableHead menuline\">Středisko</th><td class=\"i24_tableHead menuline\">Osoba</th></tr>");
			lines.stream().forEach(p -> sb.append("<tr><td class=\"i24_tableItem\">").append(p.getPerson().getDepartment())
				.append("</td><td class=\"i24_tableItem\">").append(p.getPerson().getName()).append("</td></tr>"));
		}
		sb.append("</table>");
		return sb.toString();
	}
}
