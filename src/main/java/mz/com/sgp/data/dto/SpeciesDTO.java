package mz.com.sgp.data.dto;

import java.util.Objects;

import org.springframework.hateoas.server.core.Relation;

import mz.com.sgp.config.audit.dto.AuditableDTO;

@Relation(collectionRelation = "species", itemRelation = "species")
public class SpeciesDTO extends AuditableDTO<SpeciesDTO> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String name;

	private String description;

	private CategoryDTO category;

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

	public CategoryDTO getCategory() {
		return category;
	}

	public void setCategory(CategoryDTO category) {
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
		SpeciesDTO other = (SpeciesDTO) obj;
		return Objects.equals(category, other.category) && Objects.equals(categoryId, other.categoryId)
				&& Objects.equals(description, other.description) && Objects.equals(name, other.name);
	}

}
