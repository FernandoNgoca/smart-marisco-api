package mz.com.sgp.model;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "AUTH_REFRESH_TOKEN")
public class RefreshTokenEntity {
    @Id
    @Column(name = "TOKEN_ID", length = 36)
    private String id;
    @Column(name = "USER_ID", nullable = false)
    private Long userId;
    @Column(name = "EXPIRES_AT", nullable = false)
    private Instant expiresAt;

    protected RefreshTokenEntity() {}
    public RefreshTokenEntity(String id, Long userId, Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }
}
