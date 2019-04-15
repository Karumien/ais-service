/*
 * Copyright (c) 2019-2029 Karumien s.r.o.
 *
 * Karumien s.r.o. is not responsible for defects arising from 
 * unauthorized changes to the source code.
 */
package com.karumien.cloud.ais.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.karumien.cloud.ais.api.entity.ViewPassEntity;
import com.karumien.cloud.ais.exception.NoDataFoundException;
import com.karumien.cloud.ais.repo.ViewPassRepository;

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
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public Page<ViewPassEntity> getPass(Integer userCode) {
		return userCode == null
				? passRepository.findAll(PageRequest.of(0, 50))
				: passRepository.findByUserCode(userCode, PageRequest.of(0, 50));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public List<ViewPassEntity> getPassOnsite() {
		return passRepository.findAllOnsite();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public ViewPassEntity getPassById(Integer passId) {
		return passRepository.findById(passId)
				.orElseThrow(() -> new NoDataFoundException("NO.PASS", "No Pass for ID = " + passId));
	}
}
