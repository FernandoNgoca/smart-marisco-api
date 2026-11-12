package mz.com.sgp.repository;

import java.time.LocalDateTime;
import java.util.Collection;
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

	// Filtros do histórico; cada parâmetro a null é ignorado. LEFT JOIN mantém o consumidor final.
	// O alias "client" coincide com o campo de ordenação "client.firstName", para o Spring Data reutilizar o join.
	String HISTORY_FILTER = """
			FROM SaleEntity s LEFT JOIN s.client client
			WHERE s.status = :status
			  AND (:ordersOnly = false OR s.orderRecord = true)
			  AND (:search IS NULL
			       OR LOWER(client.firstName) LIKE CONCAT('%', :search, '%')
			       OR LOWER(client.lastName) LIKE CONCAT('%', :search, '%')
			       OR s.id = :saleId)
			  AND (:saleStatus IS NULL OR s.saleStatus = :saleStatus)
			  AND (:start IS NULL OR s.createdDate >= :start)
			  AND (:end IS NULL OR s.createdDate < :end)
			""";

	@Query(value = "SELECT s " + HISTORY_FILTER, countQuery = "SELECT COUNT(s) " + HISTORY_FILTER)
	Page<SaleEntity> history(@Param("status") EntityState status, @Param("ordersOnly") boolean ordersOnly,
			@Param("search") String search, @Param("saleId") Long saleId, @Param("saleStatus") SaleStatus saleStatus,
			@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

	@Query("SELECT COUNT(s), COALESCE(SUM(CASE WHEN s.saleStatus = mz.com.sgp.model.SaleStatus.COMPLETED THEN s.totalValue ELSE 0 END), 0) "
			+ HISTORY_FILTER)
	List<Object[]> historySummary(@Param("status") EntityState status, @Param("ordersOnly") boolean ordersOnly,
			@Param("search") String search, @Param("saleId") Long saleId, @Param("saleStatus") SaleStatus saleStatus,
			@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

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

	long countByClientIdAndSaleStatusAndStatus(Long clientId, SaleStatus saleStatus, EntityState status);

	@Query("SELECT s.clientId, COUNT(s) FROM SaleEntity s WHERE s.clientId IN :clientIds AND s.saleStatus = :saleStatus AND s.status = :status GROUP BY s.clientId")
	List<Object[]> countByClientIds(@Param("clientIds") Collection<Long> clientIds, @Param("saleStatus") SaleStatus saleStatus,
			@Param("status") EntityState status);
}
