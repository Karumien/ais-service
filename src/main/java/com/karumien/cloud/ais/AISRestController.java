package com.karumien.cloud.ais;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.karumien.cloud.ais.api.handler.PassApi;
import com.karumien.cloud.ais.api.model.PassDTO;

@RestController
@RequestMapping(path = "/api")
public class AISRestController implements PassApi {

	@SuppressWarnings("serial")
	@Override
	public ResponseEntity<List<PassDTO>> getPass() {
		return new ResponseEntity<>(new ArrayList<PassDTO>() {
			{
				add(new PassDTO().id(1000).category("In"));
				add(new PassDTO().id(2000).category("Out"));
			}
		}, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<PassDTO> getPassById(String passId) {
		//PassApi.super.getPassById(passId);
		return new ResponseEntity<>(new PassDTO().id(1000).category("In"), HttpStatus.OK);
	}

}
