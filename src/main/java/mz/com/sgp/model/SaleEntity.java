package mz.com.sgp.model;

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import mz.com.sgp.config.audit.entity.AuditableEntity;

@Entity
@Table(name = "SALE")
public class SaleEntity extends AuditableEntity {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "CLIENT_ID", insertable = false, updatable = false)
	private ClientEntity client;

	@Column(name = "CLIENT_ID", nullable = true)
	private Long clientId;

	@Column(name = "TOTAL_VALUE", nullable = false)
	private BigDecimal totalValue;

	@Column(name = "SALE_STATUS", nullable = false)
	@Enumerated(EnumType.STRING)
	private SaleStatus saleStatus;

    @jakarta.persistence.Column(name = "ORDER_RECORD")
    private boolean orderRecord;
    public boolean getOrderRecord() { return orderRecord; }
    public void setOrderRecord(boolean value) { orderRecord = value; }

    @jakarta.persistence.Column(name = "STOCK_DEDUCTED")
    private boolean stockDeducted;
    public boolean getStockDeducted() { return stockDeducted; }
    public void setStockDeducted(boolean value) { stockDeducted = value; }

    @jakarta.persistence.Version
    @jakarta.persistence.Column(name = "VERSION")
    private Long version;
    @Column(name = "EDIT_REVISION")
    private long editRevision;
    public void markEdited() { editRevision++; }
    public Long getVersion() { return version; }
    public void setVersion(Long value) { version = value; }

    @jakarta.persistence.Column(name = "COMPLETED_DATE")
    private java.time.LocalDateTime completedDate;
    public java.time.LocalDateTime getCompletedDate() { return completedDate; }
    public void setCompletedDate(java.time.LocalDateTime value) { completedDate = value; }

	public ClientEntity getClient() {
		return client;
	}

	public void setClient(ClientEntity client) {
		this.client = client;
	}

	public Long getClientId() {
		return clientId;
	}

	public void setClientId(Long clientId) {
		this.clientId = clientId;
	}

	public BigDecimal getTotalValue() {
		return totalValue;
	}

	public void setTotalValue(BigDecimal totalValue) {
		this.totalValue = totalValue;
	}

	public SaleStatus getSaleStatus() {
		return saleStatus;
	}

	public void setSaleStatus(SaleStatus saleStatus) {
		this.saleStatus = saleStatus;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(client, clientId, saleStatus, totalValue);
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
		SaleEntity other = (SaleEntity) obj;
		return Objects.equals(client, other.client) && Objects.equals(clientId, other.clientId)
				&& saleStatus == other.saleStatus && Objects.equals(totalValue, other.totalValue);
	}

}
