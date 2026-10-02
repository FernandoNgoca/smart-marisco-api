package mz.com.sgp.repository;

import java.time.Instant;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import mz.com.sgp.model.RefreshTokenEntity;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, String> {
    @Modifying
    @Query("delete from RefreshTokenEntity t where t.id = :id and t.userId = :userId and t.expiresAt > :now")
    int consume(@Param("id") String id, @Param("userId") Long userId, @Param("now") Instant now);

    @Modifying
    @Query("delete from RefreshTokenEntity t where t.userId = :userId")
    void revokeForUser(@Param("userId") Long userId);

    @Modifying
    @Query("delete from RefreshTokenEntity t where t.expiresAt <= :now")
    void deleteExpired(@Param("now") Instant now);
}
