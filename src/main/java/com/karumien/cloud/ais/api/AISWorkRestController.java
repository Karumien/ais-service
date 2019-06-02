package com.karumien.cloud.ais.api;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.karumien.cloud.ais.api.handler.WorkApi;
import com.karumien.cloud.ais.api.model.WorkMonthDTO;
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
	public ResponseEntity<WorkMonthDTO> getWorkDays(Integer year, Integer month,
			@NotNull @Valid String username) {
		return new ResponseEntity<>(
			mapper.map(aisService.getWorkDays(year, month, username), WorkMonthDTO.class), HttpStatus.OK);
	}
	
}
