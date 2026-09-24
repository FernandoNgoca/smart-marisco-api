package mz.com.sgp.model;

import jakarta.persistence.*;

@Entity
@Table(name = "SALE_OPERATION", uniqueConstraints = @UniqueConstraint(columnNames = {"USER_ID", "REQUEST_KEY"}))
public class SaleOperationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "USER_ID", nullable = false)
    private Long userId;
    @Column(name = "REQUEST_KEY", nullable = false, length = 64)
    private String requestKey;
    @Column(name = "REQUEST_HASH", nullable = false, length = 64)
    private String requestHash;
    @Lob @Column(name = "RESPONSE_JSON", nullable = false, columnDefinition = "TEXT")
    private String responseJson;

    protected SaleOperationEntity() {}
    public SaleOperationEntity(Long userId, String requestKey, String requestHash, String responseJson) {
        this.userId = userId;
        this.requestKey = requestKey;
        this.requestHash = requestHash;
        this.responseJson = responseJson;
    }
    public String getRequestHash() { return requestHash; }
    public String getResponseJson() { return responseJson; }
}
