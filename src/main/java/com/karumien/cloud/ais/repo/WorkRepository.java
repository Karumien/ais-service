/*
 * Copyright (c) 2019-2029 Karumien s.r.o.
 *
 * Karumien s.r.o. is not responsible for defects arising from 
 * unauthorized changes to the source code.
 */
package com.karumien.cloud.ais.repo;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.karumien.cloud.ais.api.entity.Work;

/**
 * Repository for operations on {@link Work}.
 *
 * @author <a href="miroslav.svoboda@karumien.com">Miroslav Svoboda</a>
 * @since 1.0, 15. 4. 2019 17:10:32
 */
@Repository
public interface WorkRepository extends JpaSpecificationExecutor<Work>, JpaRepository<Work, Long> {

	/**
	 * Find saved works by user in specified month.
	 * 
	 * @param username specific user
	 * @param dateFrom date from
	 * @param dateTo   date to
	 * @return {@link List} of {@link Work} saved works by user in specified month
	 */
	@Query(value = "from Work w where w.username = :username and w.date >= :dateFrom and w.date <= :dateTo")
	List<Work> findByUsernameAndDateRange(@Param("username") String username, @Param("dateFrom") LocalDate dateFrom,
			@Param("dateTo") LocalDate dateTo);

}
