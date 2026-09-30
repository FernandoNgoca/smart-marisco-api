package mz.com.sgp.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mz.com.sgp.config.audit.entity.EntityState;
import mz.com.sgp.model.SaleEntity;
import mz.com.sgp.model.SaleStatus;

public interface SaleRepository extends JpaRepository<SaleEntity, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SaleEntity s WHERE s.id = :id AND s.status = mz.com.sgp.config.audit.entity.EntityState.ACTIVE")
    java.util.Optional<SaleEntity> lockById(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(s.totalValue), 0) FROM SaleEntity s WHERE COALESCE(s.completedDate, s.createdDate) >= :start AND COALESCE(s.completedDate, s.createdDate) < :end AND s.saleStatus = :saleStatus AND s.status = :status")
    java.math.BigDecimal sumRevenue(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
            @Param("saleStatus") SaleStatus saleStatus, @Param("status") EntityState status);


	@Query("SELECT s FROM SaleEntity s WHERE s.status = :status")
	Page<SaleEntity> findAll(Pageable pageable, @Param("status") EntityState status);

	@Query("""
			    SELECT s FROM SaleEntity s
			    WHERE (:search IS NULL
			           OR LOWER(s.client.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
			           OR LOWER(s.client.lastName) LIKE LOWER(CONCAT('%', :search, '%')))
			      AND s.status = :status
			""")
	Page<SaleEntity> search(@Param("search") String search, @Param("status") EntityState status, Pageable pageable);

	@Query("SELECT COUNT(s) FROM SaleEntity s WHERE COALESCE(s.completedDate, s.createdDate) >= :startDate AND COALESCE(s.completedDate, s.createdDate) < :endDate AND s.saleStatus = :saleStatus AND s.status = :status")
    Long countByCreatedDateBetweenAndSaleStatusAndStatus(LocalDateTime startDate, LocalDateTime endDate,
			SaleStatus saleStatus, EntityState status);

	@Query("""
			SELECT FUNCTION('DAYOFWEEK', COALESCE(s.completedDate, s.createdDate)), COUNT(s.id)
			FROM SaleEntity s
			WHERE COALESCE(s.completedDate, s.createdDate) BETWEEN :start AND :end
			AND s.saleStatus = :saleStatus
			AND s.status = :status
			GROUP BY FUNCTION('DAYOFWEEK', COALESCE(s.completedDate, s.createdDate))
			""")
	List<Object[]> findSalesByWeek(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
			@Param("saleStatus") SaleStatus saleStatus, @Param("status") EntityState status);

	@Query("""
			    SELECT COUNT(s.id)
			    FROM SaleEntity s
			    WHERE COALESCE(s.completedDate, s.createdDate) >= :start
			    AND COALESCE(s.completedDate, s.createdDate) < :end
			    AND s.saleStatus = :saleStatus
			    AND s.status = :status
			""")
	Long countSalesCurrentMonth(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
			@Param("saleStatus") SaleStatus saleStatus, @Param("status") EntityState status);

	@Query("""
			    SELECT COUNT(s.id)
			    FROM SaleEntity s
			    WHERE COALESCE(s.completedDate, s.createdDate) >= :start
			    AND COALESCE(s.completedDate, s.createdDate) < :end
			    AND s.saleStatus = :saleStatus
			    AND s.status = :status
			""")
	Long countSalesCurrentDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
			@Param("saleStatus") SaleStatus saleStatus, @Param("status") EntityState status);

	@Query("SELECT s FROM SaleEntity s WHERE s.status = :status AND s.orderRecord = true")
	Page<SaleEntity> findAllOrders(Pageable pageable, @Param("status") EntityState status);

	@Query("""
			    SELECT s FROM SaleEntity s
			    WHERE (:search IS NULL
			           OR LOWER(s.client.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
			           OR LOWER(s.client.lastName) LIKE LOWER(CONCAT('%', :search, '%')))
			      AND s.status = :status
			      AND s.orderRecord = true
			""")
	Page<SaleEntity> searchOrders(@Param("search") String search, @Param("status") EntityState status,
			Pageable pageable);
	
	long countByStatusAndSaleStatus(EntityState status, SaleStatus saleStatus);
}
