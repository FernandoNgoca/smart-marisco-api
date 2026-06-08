package mz.com.sgp.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mz.com.sgp.model.PermissionEntity;

public interface PermissionRepository extends JpaRepository<PermissionEntity, Long> {
	
	List<PermissionEntity> findByDescriptionIn(List<String> descriptions);

 

}
