package mz.com.sgp.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mz.com.sgp.config.audit.entity.EntityState;
import mz.com.sgp.model.ProductEntity;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

	@Query("SELECT p FROM ProductEntity p WHERE p.status = :status")
	Page<ProductEntity> findAll(Pageable pageable, @Param("status") EntityState status);

	ProductEntity findFirstByOrderByIdDesc();

	@Query("""
			    SELECT p FROM ProductEntity p
			    WHERE p.status = :status
			    AND (
			        LOWER(p.name) LIKE %:search%
			        OR LOWER(p.code) LIKE %:search%
			        OR LOWER(p.species.name) LIKE %:search%
			    )
			""")
	Page<ProductEntity> search(@Param("search") String search, @Param("status") EntityState status, Pageable pageable);

	long countByStatus(EntityState status);

	@Query(value = """
			SELECT p.*
			FROM PRODUCT p
			LEFT JOIN STOCK s ON s.product_id = p.id
			WHERE s.product_id IS NULL AND p.status = :status
			""", nativeQuery = true)
	List<ProductEntity> findProductsWithoutStock(@Param("status") EntityState status);

	@Query("""
			SELECT DISTINCT s.product
			FROM StockEntity s
			WHERE s.quantity > 0 AND s.status = :status
			""")
	List<ProductEntity> findAvailableProducts(@Param("status") EntityState status);

}
