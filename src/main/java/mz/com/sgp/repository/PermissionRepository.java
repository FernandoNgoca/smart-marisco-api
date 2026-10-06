package mz.com.sgp.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mz.com.sgp.model.PermissionEntity;

public interface PermissionRepository extends JpaRepository<PermissionEntity, Long> {
	
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT p FROM PermissionEntity p WHERE p.description = 'ROLE_ADMIN'")
    java.util.Optional<PermissionEntity> lockAdminRole();
	List<PermissionEntity> findByDescriptionIn(List<String> descriptions);

 

}
