package mz.com.sgp.model;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import mz.com.sgp.config.audit.entity.AuditableEntity;

@Entity
@Table(name = "SPECIES")
public class SpeciesEntity extends AuditableEntity {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Column(name = "NAME", nullable = false)
	private String name;

	@Column(name = "DESCRIPTION", nullable = false)
	private String description;

	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "CATEGORY_ID", insertable = false, updatable = false, nullable = false)
	private CategoryEntity category;

	@Column(name = "CATEGORY_ID", nullable = false)
	private Long categoryId;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public CategoryEntity getCategory() {
		return category;
	}

	public void setCategory(CategoryEntity category) {
		this.category = category;
	}

	public Long getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(Long categoryId) {
		this.categoryId = categoryId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(category, categoryId, description, name);
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
		SpeciesEntity other = (SpeciesEntity) obj;
		return Objects.equals(category, other.category) && Objects.equals(categoryId, other.categoryId)
				&& Objects.equals(description, other.description) && Objects.equals(name, other.name);
	}

}
