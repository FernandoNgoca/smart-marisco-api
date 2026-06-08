package mz.com.sgp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mz.com.sgp.config.audit.entity.EntityState;
import mz.com.sgp.model.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

	@Query("SELECT u FROM UserEntity u WHERE u.userName =:userName")
	UserEntity findByUsername(@Param("userName") String userName);

	@Query("SELECT p FROM UserEntity p WHERE p.status = :status")
	Page<UserEntity> findAll(Pageable pageable, @Param("status") EntityState status);

	@Query("""
			    SELECT u FROM UserEntity u
			    WHERE u.status = :status
			    AND (
			        LOWER(u.userName) LIKE %:search%
			        OR LOWER(u.fullName) LIKE %:search%
			    )
			""")
	Page<UserEntity> search(@Param("search") String search, @Param("status") EntityState status, Pageable pageable);

}
