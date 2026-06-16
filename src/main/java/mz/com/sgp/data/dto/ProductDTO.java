package mz.com.sgp.data.dto;

import java.math.BigDecimal;
import java.util.Objects;

import org.springframework.hateoas.server.core.Relation;

import mz.com.sgp.config.audit.dto.AuditableDTO;

@Relation(collectionRelation = "products", itemRelation = "product")
public class ProductDTO extends AuditableDTO<ProductDTO> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String code;

	private String name;

	private String description;

	private BigDecimal price;

	private BigDecimal salePrice;

	private SpeciesDTO species;

	private Long speciesId;

	private String image;

	private UnitDTO unit;

	private Long unitId;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

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

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public BigDecimal getSalePrice() {
		return salePrice;
	}

	public void setSalePrice(BigDecimal salePrice) {
		this.salePrice = salePrice;
	}

	public SpeciesDTO getSpecies() {
		return species;
	}

	public void setSpecies(SpeciesDTO species) {
		this.species = species;
	}

	public Long getSpeciesId() {
		return speciesId;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public void setSpeciesId(Long speciesId) {
		this.speciesId = speciesId;
	}

	public UnitDTO getUnit() {
		return unit;
	}

	public void setUnit(UnitDTO unit) {
		this.unit = unit;
	}

	public Long getUnitId() {
		return unitId;
	}

	public void setUnitId(Long unitId) {
		this.unitId = unitId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ Objects.hash(code, description, image, name, price, salePrice, species, speciesId, unit, unitId);
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
		ProductDTO other = (ProductDTO) obj;
		return Objects.equals(code, other.code) && Objects.equals(description, other.description)
				&& Objects.equals(image, other.image) && Objects.equals(name, other.name)
				&& Objects.equals(price, other.price) && Objects.equals(salePrice, other.salePrice)
				&& Objects.equals(species, other.species) && Objects.equals(speciesId, other.speciesId)
				&& Objects.equals(unit, other.unit) && Objects.equals(unitId, other.unitId);
	}

}
