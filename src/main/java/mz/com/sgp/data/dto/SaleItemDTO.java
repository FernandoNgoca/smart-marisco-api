package mz.com.sgp.data.dto;

import java.math.BigDecimal;
import java.util.Objects;

import org.springframework.hateoas.server.core.Relation;

import mz.com.sgp.config.audit.dto.AuditableDTO;

@Relation(collectionRelation = "saleItems", itemRelation = "saleItem")
public class SaleItemDTO extends AuditableDTO<SaleItemDTO> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Long productId;

	private ProductDTO product;

	private BigDecimal quantity;

	private SaleDTO sale;

	private Long saleId;

	public Long getProductId() {
		return productId;
	}

	public void setProductId(Long productId) {
		this.productId = productId;
	}

	public ProductDTO getProduct() {
		return product;
	}

	public void setProduct(ProductDTO product) {
		this.product = product;
	}

	public SaleDTO getSale() {
		return sale;
	}

	public void setSale(SaleDTO sale) {
		this.sale = sale;
	}

	public Long getSaleId() {
		return saleId;
	}

	public void setSaleId(Long saleId) {
		this.saleId = saleId;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(product, productId, quantity, sale, saleId);
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
		SaleItemDTO other = (SaleItemDTO) obj;
		return Objects.equals(product, other.product) && Objects.equals(productId, other.productId)
				&& Objects.equals(quantity, other.quantity) && Objects.equals(sale, other.sale)
				&& Objects.equals(saleId, other.saleId);
	}

}
