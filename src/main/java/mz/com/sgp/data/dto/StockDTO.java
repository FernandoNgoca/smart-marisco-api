package mz.com.sgp.data.dto;

import java.math.BigDecimal;
import java.util.Objects;

import org.springframework.hateoas.server.core.Relation;

import mz.com.sgp.config.audit.dto.AuditableDTO;

@Relation(collectionRelation = "stock", itemRelation = "stock")
public class StockDTO extends AuditableDTO<StockDTO> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private ProductDTO product;

	private Long productId;

	private BigDecimal quantity;

	private String description;

	public StockDTO() {
		super();
	}

	public ProductDTO getProduct() {
		return product;
	}

	public void setProduct(ProductDTO product) {
		this.product = product;
	}

	public Long getProductId() {
		return productId;
	}

	public void setProductId(Long productId) {
		this.productId = productId;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(product, productId, quantity);
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
		StockDTO other = (StockDTO) obj;
		return Objects.equals(product, other.product) && Objects.equals(productId, other.productId)
				&& Objects.equals(quantity, other.quantity);
	}

}
