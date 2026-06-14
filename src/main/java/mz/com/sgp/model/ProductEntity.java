package mz.com.sgp.model;

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import mz.com.sgp.config.audit.entity.AuditableEntity;

@Entity
@Table(name = "PRODUCT")
public class ProductEntity extends AuditableEntity {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Column(name = "NAME", nullable = false)
	private String name;

	@Column(name = "DESCRIPTION", nullable = false)
	private String description;

	@Column(name = "PRICE", nullable = false)
	private BigDecimal price;

	@Column(name = "SALE_PRICE", nullable = false)
	private BigDecimal salePrice;

	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "SPECIES_ID", insertable = false, updatable = false, nullable = false)
	private SpeciesEntity species;

	@Column(name = "SPECIES_ID", nullable = false)
	private Long speciesId;

	@Column(name = "CODE", unique = true, nullable = false)
	private String code;

	@Column(name = "IMAGE", nullable = true)
	private String image;

	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "UNIT_ID", insertable = false, updatable = false, nullable = false)
	private UnitEntity unit;

	@Column(name = "UNIT_ID", nullable = false)
	private Long unitId;

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

	public SpeciesEntity getSpecies() {
		return species;
	}

	public void setSpecies(SpeciesEntity species) {
		this.species = species;
	}

	public Long getSpeciesId() {
		return speciesId;
	}

	public void setSpeciesId(Long speciesId) {
		this.speciesId = speciesId;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public UnitEntity getUnit() {
		return unit;
	}

	public void setUnit(UnitEntity unit) {
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
		ProductEntity other = (ProductEntity) obj;
		return Objects.equals(code, other.code) && Objects.equals(description, other.description)
				&& Objects.equals(image, other.image) && Objects.equals(name, other.name)
				&& Objects.equals(price, other.price) && Objects.equals(salePrice, other.salePrice)
				&& Objects.equals(species, other.species) && Objects.equals(speciesId, other.speciesId)
				&& Objects.equals(unit, other.unit) && Objects.equals(unitId, other.unitId);
	}

}
