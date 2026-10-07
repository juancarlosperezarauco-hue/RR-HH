package com.hrcoresystem.hrcore_api.modules.platform.billing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionCheckoutSessionJpaRepository extends JpaRepository<SubscriptionCheckoutSessionEntity, UUID> {

    Optional<SubscriptionCheckoutSessionEntity> findByStripeSessionId(String stripeSessionId);

    Optional<SubscriptionCheckoutSessionEntity> findByStripeSubscriptionId(String stripeSubscriptionId);

    List<SubscriptionCheckoutSessionEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    @Query("""
        select s
        from SubscriptionCheckoutSessionEntity s
        where s.status in ('PENDING', 'OPEN', 'CREATED')
          and s.stripeSessionId is not null
        order by s.createdAt asc
    """)
    List<SubscriptionCheckoutSessionEntity> findPendingActivationCandidates();
}
