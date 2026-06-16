package mz.com.sgp.model;

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import mz.com.sgp.config.audit.entity.AuditableEntity;

@Entity
@Table(name = "SALE_ITEM")
public class SaleItemEntity extends AuditableEntity {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PRODUCT_ID", insertable = false, updatable = false, nullable = false)
	private ProductEntity product;

	@Column(name = "PRODUCT_ID", nullable = false)
	private Long productId;

	@Column(name = "QUANTITY", nullable = false)
	private BigDecimal quantity;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "SALE_ID", insertable = false, updatable = false, nullable = false)
	private SaleEntity sale;

	@Column(name = "SALE_ID", nullable = false)
	private Long saleId;

	public ProductEntity getProduct() {
		return product;
	}

	public void setProduct(ProductEntity product) {
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

	public SaleEntity getSale() {
		return sale;
	}

	public void setSale(SaleEntity sale) {
		this.sale = sale;
	}

	public Long getSaleId() {
		return saleId;
	}

	public void setSaleId(Long saleId) {
		this.saleId = saleId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(product, productId, quantity, sale, saleId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SaleItemEntity other = (SaleItemEntity) obj;
		return Objects.equals(product, other.product) && Objects.equals(productId, other.productId)
				&& Objects.equals(quantity, other.quantity) && Objects.equals(sale, other.sale)
				&& Objects.equals(saleId, other.saleId);
	}

}
