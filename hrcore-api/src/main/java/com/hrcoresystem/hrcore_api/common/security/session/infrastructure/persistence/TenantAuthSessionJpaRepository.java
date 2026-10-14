package com.hrcoresystem.hrcore_api.common.security.session.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface TenantAuthSessionJpaRepository extends JpaRepository<TenantAuthSessionEntity, UUID> {
    Optional<TenantAuthSessionEntity> findByRefreshTokenHash(String hash);
    List<TenantAuthSessionEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update TenantAuthSessionEntity e set e.revokedAt = CURRENT_TIMESTAMP where e.id = :sessionId and e.userId = :userId and e.revokedAt is null")
    int revokeOwnedSession(@Param("sessionId") UUID sessionId, @Param("userId") UUID userId);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update TenantAuthSessionEntity e set e.revokedAt = CURRENT_TIMESTAMP where e.userId = :userId and e.id <> :currentSessionId and e.revokedAt is null")
    int revokeOtherSessions(@Param("userId") UUID userId, @Param("currentSessionId") UUID currentSessionId);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update TenantAuthSessionEntity e set e.revokedAt = CURRENT_TIMESTAMP where e.userId = :userId and e.revokedAt is null")
    int revokeAllByUserId(@Param("userId") UUID userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from TenantAuthSessionEntity e where e.id = :id")
    Optional<TenantAuthSessionEntity> findLockedById(@Param("id") UUID id);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update TenantAuthSessionEntity e set e.refreshTokenHash = :newHash, e.refreshGeneration = :newGeneration, e.lastUsedAt = :now, e.lastRotatedAt = :now where e.id = :sessionId and e.userId = :userId and e.refreshGeneration = :expectedGeneration and e.refreshTokenHash = :expectedHash and e.revokedAt is null")
    int rotate(@Param("sessionId") UUID sessionId, @Param("userId") UUID userId,
               @Param("expectedGeneration") long expectedGeneration, @Param("expectedHash") String expectedHash,
               @Param("newHash") String newHash, @Param("newGeneration") long newGeneration,
               @Param("now") java.time.Instant now);
}
