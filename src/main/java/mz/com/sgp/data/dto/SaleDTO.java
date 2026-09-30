package mz.com.sgp.data.dto;

import java.math.BigDecimal;
import java.util.Objects;

import org.springframework.hateoas.server.core.Relation;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import mz.com.sgp.config.audit.dto.AuditableDTO;
import mz.com.sgp.model.SaleStatus;

@Relation(collectionRelation = "sales", itemRelation = "sale")
public class SaleDTO extends AuditableDTO<SaleDTO> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private ClientDTO client;

	private Long clientId;

	private BigDecimal totalValue;

	@Enumerated(EnumType.STRING)
	private SaleStatus saleStatus;

    private boolean orderRecord;
    public boolean getOrderRecord() { return orderRecord; }
    public void setOrderRecord(boolean value) { orderRecord = value; }

    private boolean stockDeducted;
    public boolean getStockDeducted() { return stockDeducted; }
    public void setStockDeducted(boolean value) { stockDeducted = value; }

    private Long version;
    public Long getVersion() { return version; }
    public void setVersion(Long value) { version = value; }

    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer.class)
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer.class)
    private java.time.LocalDateTime completedDate;
    public java.time.LocalDateTime getCompletedDate() { return completedDate; }
    public void setCompletedDate(java.time.LocalDateTime value) { completedDate = value; }

	public ClientDTO getClient() {
		return client;
	}

	public void setClient(ClientDTO client) {
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
		SaleDTO other = (SaleDTO) obj;
		return Objects.equals(client, other.client) && Objects.equals(clientId, other.clientId)
				&& saleStatus == other.saleStatus && Objects.equals(totalValue, other.totalValue);
	}

}
