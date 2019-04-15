/*
 * Copyright (c) 2019-2029 Karumien s.r.o.
 *
 * Karumien s.r.o. is not responsible for defects arising from 
 * unauthorized changes to the source code.
 */
package com.karumien.cloud.ais.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Base No Data Exception.
 *
 * @author <a href="miroslav.svoboda@karumien.com">Miroslav Svoboda</a>
 * @since 1.0, 15. 4. 2019 18:36:30 
 */
@Data
@EqualsAndHashCode(callSuper = false, of = "code")
@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class NoDataFoundException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;

	private String code;
	
	public NoDataFoundException() {
	}

	public NoDataFoundException(String code, String message) {
		super(message);
		this.code = code;
	}

	public NoDataFoundException(String code, Throwable cause) {
		super(cause);
		this.code = code;
	}

	public NoDataFoundException(String code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}

}
