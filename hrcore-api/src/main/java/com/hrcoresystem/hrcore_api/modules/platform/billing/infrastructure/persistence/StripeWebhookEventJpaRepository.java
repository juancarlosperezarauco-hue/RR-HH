package com.hrcoresystem.hrcore_api.modules.platform.billing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StripeWebhookEventJpaRepository extends JpaRepository<StripeWebhookEventEntity, UUID> {

    Optional<StripeWebhookEventEntity> findByStripeEventId(String stripeEventId);

    boolean existsByStripeEventId(String stripeEventId);
}
