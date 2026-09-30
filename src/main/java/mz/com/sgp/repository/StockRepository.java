package mz.com.sgp.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mz.com.sgp.config.audit.entity.EntityState;
import mz.com.sgp.model.StockEntity;

public interface StockRepository extends JpaRepository<StockEntity, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StockEntity s WHERE s.productId = :productId AND s.status = mz.com.sgp.config.audit.entity.EntityState.ACTIVE")
    Optional<StockEntity> lockByProductId(@Param("productId") Long productId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StockEntity s WHERE s.id = :id AND s.status = mz.com.sgp.config.audit.entity.EntityState.ACTIVE")
    Optional<StockEntity> lockById(@Param("id") Long id);


	@Query("SELECT p FROM StockEntity p WHERE p.status = :status")
	Page<StockEntity> findAll(Pageable pageable, @Param("status") EntityState status);

	@Query("SELECT s FROM StockEntity s WHERE s.productId = :productId AND s.status = :status")
	Optional<StockEntity> findFirstByProductIdAndStatus(@Param("productId") Long productId,
			@Param("status") EntityState status);

	@Query("SELECT s FROM StockEntity s WHERE s.id = :id AND s.status = :status")
	Optional<StockEntity> findFirstByIdAndStatus(@Param("id") Long productId, @Param("status") EntityState status);

	@Query("""
			    SELECT s FROM StockEntity s
			    WHERE (:search IS NULL
			           OR LOWER(s.product.name) LIKE LOWER(CONCAT('%', :search, '%')))
			      AND s.status = :status
			""")
	Page<StockEntity> search(@Param("search") String search, @Param("status") EntityState status, Pageable pageable);
}
