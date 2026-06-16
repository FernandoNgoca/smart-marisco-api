package mz.com.sgp.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mz.com.sgp.config.audit.entity.EntityState;
import mz.com.sgp.model.SpeciesEntity;

public interface SpeciesRepository extends JpaRepository<SpeciesEntity, Long> {

	@Query("SELECT s FROM SpeciesEntity s WHERE s.status = :status")
	Page<SpeciesEntity> findAll(Pageable pageable, @Param("status") EntityState status);

	@Query("""
			    SELECT s FROM SpeciesEntity s
			    WHERE (
			        :search IS NULL OR
			        LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
			        LOWER(s.description) LIKE LOWER(CONCAT('%', :search, '%'))
			    )
			    AND s.status = :status
			""")
	Page<SpeciesEntity> search(@Param("search") String search, @Param("status") EntityState status, Pageable pageable);
	
	List<SpeciesEntity> findByCategoryId(Long categoryId);
}
