/*
 * Copyright (c) 2019-2029 Karumien s.r.o.
 *
 * Karumien s.r.o. is not responsible for defects arising from 
 * unauthorized changes to the source code.
 */
package com.karumien.cloud.ais.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.karumien.cloud.ais.api.entity.UserInfo;

/**
 * Repository for operations on {@link UserInfo}.
 *
 * @author <a href="miroslav.svoboda@karumien.com">Miroslav Svoboda</a>
 * @since 1.0, 15. 4. 2019 17:10:32
 */
@Repository
public interface UserInfoRepository extends JpaSpecificationExecutor<UserInfo>, JpaRepository<UserInfo, Long> {
	
	/**
	 * Find known user by username.
	 * 
	 * @param username specific user
	 * @return {@link List} of {@link UserInfo} know user by username
	 */
	@Query(value = "from UserInfo u where u.username = :username")
	Optional<UserInfo> findByUsername(@Param("username") String username);

	/**
	 * Find all known users ordered by username.
	 * 
	 * @return {@link List} of {@link UserInfo} all known users ordered by username
	 */
	@Query(value = "from UserInfo u order by u.username")
	List<UserInfo> findAllOrderByUsername();
	
}
