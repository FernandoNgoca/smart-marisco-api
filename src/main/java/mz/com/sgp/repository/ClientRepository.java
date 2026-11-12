package mz.com.sgp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mz.com.sgp.config.audit.entity.EntityState;
import mz.com.sgp.model.ClientEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface ClientRepository extends JpaRepository<ClientEntity, Long> {
	// Bloqueia o cliente para que desativar e registar um pedido não aconteçam ao mesmo tempo.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT c FROM ClientEntity c WHERE c.id = :id")
	Optional<ClientEntity> lockById(@Param("id") Long id);


	@Query("SELECT p FROM ClientEntity p WHERE p.status = :status")
	Page<ClientEntity> findAll(Pageable pageable, @Param("status") EntityState status);

	@Query("SELECT c FROM ClientEntity c WHERE c.firstName LIKE LOWER(CONCAT ('%', :firstName, '%'))")
	Page<ClientEntity> findClientByName(@Param("firstName") String firstName, Pageable pageable);

	@Query("""
			    SELECT c FROM ClientEntity c
			    WHERE c.status = :status
			    AND (
			        LOWER(c.firstName) LIKE %:search%
			        OR LOWER(c.lastName) LIKE %:search%
			        OR LOWER(c.address) LIKE %:search%
			    )
			""")
	Page<ClientEntity> search(@Param("search") String search, @Param("status") EntityState status, Pageable pageable);

	long countByStatus(EntityState status);

}
