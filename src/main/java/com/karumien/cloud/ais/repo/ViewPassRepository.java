/*
 * Copyright (c) 2019-2029 Karumien s.r.o.
 *
 * Karumien s.r.o. is not responsible for defects arising from 
 * unauthorized changes to the source code.
 */
package com.karumien.cloud.ais.repo;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.karumien.cloud.ais.api.entity.PassEntity;
import com.karumien.cloud.ais.api.entity.ViewPassEntity;

/**
 * Repository for operations on {@link PassEntity} or {@link ViewPassEntity}. 
 *
 * @author <a href="miroslav.svoboda@karumien.com">Miroslav Svoboda</a>
 * @since 1.0, 15. 4. 2019 17:10:32
 */
@Repository
public interface ViewPassRepository extends JpaSpecificationExecutor<ViewPassEntity>, JpaRepository<ViewPassEntity, Integer> {

	/** 
	 * Returns all users onsite.
	 * 
	 * @return {@link List} of {@link ViewPassEntity} which is onsite
	 */
	@Query(nativeQuery = true, value = "select * from VIEW_DATPRUCHUDAL_LAST order by ACTION_TYPE")
	List<ViewPassEntity> findAllOnsite();

	/**
	 * Find last passes for filtered user.
	 * 
	 * @param userCode usercode filtered
	 * @param page pageable context
	 * @return {@link Page} of {@link ViewPassEntity} for filtered user
	 */
	@Query(value = "from ViewPassEntity v where v.person.code = :userCode")
	Page<ViewPassEntity> findByUserCode(@Param("userCode") Integer userCode, Pageable page);
	
}
