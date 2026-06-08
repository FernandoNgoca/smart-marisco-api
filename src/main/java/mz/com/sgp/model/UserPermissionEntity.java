package mz.com.sgp.model;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import mz.com.sgp.config.audit.entity.AuditableEntity;

@Entity
@Table(name = "user_permission")
public class UserPermissionEntity extends AuditableEntity {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Column(name = "ID_USER")
	private Long userId;

	@Column(name = "ID_PERMISSION")
	private Long permissionId;

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Long getPermissionId() {
		return permissionId;
	}

	public void setPermissionId(Long permissionId) {
		this.permissionId = permissionId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(permissionId, userId);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserPermissionEntity other = (UserPermissionEntity) obj;
		return Objects.equals(permissionId, other.permissionId) && Objects.equals(userId, other.userId);
	}
	
	

}