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

import com.karumien.cloud.ais.api.entity.Pass;
import com.karumien.cloud.ais.api.entity.ViewPass;

/**
 * Repository for operations on {@link Pass} or {@link ViewPass}. 
 *
 * @author <a href="miroslav.svoboda@karumien.com">Miroslav Svoboda</a>
 * @since 1.0, 15. 4. 2019 17:10:32
 */
@Repository
public interface ViewPassRepository extends JpaSpecificationExecutor<ViewPass>, JpaRepository<ViewPass, Integer> {

	/** 
	 * Returns all users onsite.
	 * 
	 * @return {@link List} of {@link ViewPass} which is onsite
	 */
	@Query(nativeQuery = true, value = "select * from VIEW_DATPRUCHUDAL_LAST order by ACTION_TYPE")
	List<ViewPass> findAllOnsite();

	/**
	 * Find last passes for filtered user by username.
	 * 
	 * @param userCode usercode filtered
	 * @param page pageable context
	 * @return {@link Page} of {@link ViewPass} for filtered user
	 */
	@Query(value = "from ViewPass v where v.person.code = :userCode")
	Page<ViewPass> findByUserCode(@Param("userCode") Integer userCode, Pageable page);
	
	/**
	 * Find last passes for filtered user by username.
	 * 
	 * @param username username filtered
	 * @param page pageable context
	 * @return {@link Page} of {@link ViewPass} for filtered user
	 */
	@Query(value = "from ViewPass v where v.person.username = :username")
	Page<ViewPass> findByUsername(@Param("username") String username, Pageable page);

	/**
	 * Find passes for selected work month for filtered user
	 * 
	 * @param username username filtered
	 * @param year selected year
	 * @param month selected month
	 * @return {@link Page} of {@link ViewPass} for filtered user
	 */
	@Query(value = "from ViewPass v where v.person.username = :username and v.year = :year and v.month = :month order by v.date")
	List<ViewPass> findByUsernameAndMonth(@Param("username") String username, Integer year, Integer month);

}
