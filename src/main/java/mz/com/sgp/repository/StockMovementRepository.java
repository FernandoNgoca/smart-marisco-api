package mz.com.sgp.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mz.com.sgp.config.audit.entity.EntityState;
import mz.com.sgp.model.StockMovementEntity;

public interface StockMovementRepository extends JpaRepository<StockMovementEntity, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<StockMovementEntity> {

	@Query("SELECT sm FROM StockMovementEntity sm WHERE sm.status = :status")
	Page<StockMovementEntity> findAll(Pageable pageable, @Param("status") EntityState status);

    @Query("SELECT sm FROM StockMovementEntity sm WHERE sm.stock.product.id = :productId AND sm.status = :state")
	Page<StockMovementEntity> findByStockIdAndStatus(@Param("productId") Long productId, @Param("state") EntityState state, Pageable pageable);

    interface Totals {
        Long getMovements();
        java.math.BigDecimal getEntries();
        java.math.BigDecimal getExits();
    }
    @Query("SELECT count(sm) AS movements, coalesce(sum(case when sm.type = mz.com.sgp.model.MovementType.ENTRY then sm.quantity else 0 end), 0) AS entries, coalesce(sum(case when sm.type = mz.com.sgp.model.MovementType.EXIT then sm.quantity else 0 end), 0) AS exits FROM StockMovementEntity sm WHERE sm.stock.product.id = :productId AND sm.status = mz.com.sgp.config.audit.entity.EntityState.ACTIVE")
    Totals totals(@Param("productId") Long productId);

	List<StockMovementEntity> findByStockIdAndStatus(Long stockId, EntityState status);
}
