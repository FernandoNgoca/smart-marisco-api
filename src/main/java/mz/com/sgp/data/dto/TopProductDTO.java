package mz.com.sgp.data.dto;

import java.math.BigDecimal;

public class TopProductDTO {

	private Long id;

	private String name;

	private String image;

	private BigDecimal salePrice;

	private BigDecimal quantity;

	private BigDecimal totalValue;
	
	private UnitDTO unit;

	public TopProductDTO(Long id, String name, String image, BigDecimal salePrice, BigDecimal quantity,
			BigDecimal totalValue) {
		super();
		this.id = id;
		this.name = name;
		this.image = image;
		this.salePrice = salePrice;
		this.quantity = quantity;
		this.totalValue = totalValue;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public BigDecimal getSalePrice() {
		return salePrice;
	}

	public void setSalePrice(BigDecimal salePrice) {
		this.salePrice = salePrice;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}

	public BigDecimal getTotalValue() {
		return totalValue;
	}

	public void setTotalValue(BigDecimal totalValue) {
		this.totalValue = totalValue;
	}

	public UnitDTO getUnit() {
		return unit;
	}

	public void setUnit(UnitDTO unit) {
		this.unit = unit;
	}

}
