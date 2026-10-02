package mz.com.sgp.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import mz.com.sgp.model.SaleOperationEntity;

public interface SaleOperationRepository extends JpaRepository<SaleOperationEntity, Long> {
    Optional<SaleOperationEntity> findByUserIdAndRequestKey(Long userId, String requestKey);
}
