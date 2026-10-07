package com.hrcoresystem.hrcore_api.modules.platform.subscriptions.infrastructure.persistence;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface PlatformSubscriptionJpaRepository extends JpaRepository<PlatformSubscriptionEntity, UUID> {

    Optional<PlatformSubscriptionEntity> findByStripeSubscriptionId(String stripeSubscriptionId);

    Optional<PlatformSubscriptionEntity> findByTenantIdAndCurrentSubscriptionTrue(UUID tenantId);

    @Modifying
    @Transactional
    @Query("""
            update PlatformSubscriptionEntity s
               set s.currentSubscription = false,
                   s.updatedAt = CURRENT_TIMESTAMP
             where s.tenantId = :tenantId
               and s.currentSubscription = true
            """)
    void clearCurrentSubscriptions(UUID tenantId);
}
