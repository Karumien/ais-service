/*
 * Copyright (c) 2019-2029 Karumien s.r.o.
 *
 * Karumien s.r.o. is not responsible for defects arising from 
 * unauthorized changes to the source code.
 */
package com.karumien.cloud.ais.api;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.karumien.cloud.ais.exception.ExceptionErrorResponse;
import com.karumien.cloud.ais.exception.NoDataFoundException;

/**
 * Global Exception Handler.
 * 
 * @author <a href="miroslav.svoboda@karumien.com">Miroslav Svoboda</a>
 * @since 1.0, 15. 4. 2019 18:39:06
 */
@RestControllerAdvice
public class AISExceptionHandler {

	@ExceptionHandler(NoDataFoundException.class)
	public ResponseEntity<ExceptionErrorResponse> exceptionHandler(NoDataFoundException e) {
		ResponseStatus status = AnnotationUtils.findAnnotation(e.getClass(), ResponseStatus.class);
		if (status != null) {
			return new ResponseEntity<>(new ExceptionErrorResponse(e.getCode(), e.getMessage()), status.code());
		}
		return new ResponseEntity<>(new ExceptionErrorResponse("NO.GLOBAL", e.getMessage()), HttpStatus.NOT_FOUND);
	}

}
