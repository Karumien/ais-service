/*
 * Copyright (c) 2019-2029 Karumien s.r.o.
 *
 * Karumien s.r.o. is not responsible for defects arising from 
 * unauthorized changes to the source code.
 */
package com.karumien.cloud.ais.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.karumien.cloud.ais.api.entity.ViewPassEntity;
import com.karumien.cloud.ais.api.model.PassDTO;

/**
 * Front AIS Service.
 *
 * @author <a href="miroslav.svoboda@karumien.com">Miroslav Svoboda</a>
 * @since 1.0, 15. 4. 2019 17:09:02
 */
public interface AISService {

	/**
	 * Returns passes filtered by user (optional).
	 * 
	 * @param usercode filtered by usercode (optional)
	 * @return {@link Page} of {@link PassDTO} filtered by optional user
	 */
	Page<ViewPassEntity> getPass(Integer usercode);

	/**
	 * Returns all users onsite.
	 * 
	 * @return {@link List} of {@link PassDTO} which is onsite
	 */
	List<ViewPassEntity> getPassOnsite();

	/**
	 * Returns specified {@link PassDTO} by {@code id}.
	 * 
	 * @param passId id of pass
	 * @return {@link PassDTO} specified by {@code id}
	 */
	ViewPassEntity getPassById(Integer passId);

}
