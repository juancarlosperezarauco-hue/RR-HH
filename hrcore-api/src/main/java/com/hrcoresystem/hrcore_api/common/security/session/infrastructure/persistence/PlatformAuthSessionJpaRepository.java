package com.hrcoresystem.hrcore_api.common.security.session.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface PlatformAuthSessionJpaRepository extends JpaRepository<PlatformAuthSessionEntity, UUID> {
    Optional<PlatformAuthSessionEntity> findByRefreshTokenHash(String hash);
    List<PlatformAuthSessionEntity> findBySuperadminIdOrderByCreatedAtDesc(UUID superadminId);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update PlatformAuthSessionEntity e set e.revokedAt = CURRENT_TIMESTAMP where e.id = :sessionId and e.superadminId = :superadminId and e.revokedAt is null")
    int revokeOwnedSession(@Param("sessionId") UUID sessionId, @Param("superadminId") UUID superadminId);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update PlatformAuthSessionEntity e set e.revokedAt = CURRENT_TIMESTAMP where e.superadminId = :superadminId and e.id <> :currentSessionId and e.revokedAt is null")
    int revokeOtherSessions(@Param("superadminId") UUID superadminId, @Param("currentSessionId") UUID currentSessionId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from PlatformAuthSessionEntity e where e.id = :id")
    Optional<PlatformAuthSessionEntity> findLockedById(@Param("id") UUID id);
}
