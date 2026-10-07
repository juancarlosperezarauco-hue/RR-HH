package com.hrcoresystem.hrcore_api.modules.identity.auth.infrastructure.persistence;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TenantAccountActivationTokenJpaRepository extends JpaRepository<TenantAccountActivationTokenEntity, UUID> {

    Optional<TenantAccountActivationTokenEntity> findByToken(String token);

    @Modifying
    @Transactional
    @Query("""
            update TenantAccountActivationTokenEntity t
               set t.used = true,
                   t.usedAt = :usedAt
             where t.email = :email
               and t.used = false
            """)
    void invalidateAllByEmail(String email, Instant usedAt);

    @Modifying
    @Transactional
    @Query("""
            update TenantAccountActivationTokenEntity t
               set t.used = true,
                   t.usedAt = :usedAt
             where t.token = :token
               and t.used = false
            """)
    void markUsed(String token, Instant usedAt);
}
