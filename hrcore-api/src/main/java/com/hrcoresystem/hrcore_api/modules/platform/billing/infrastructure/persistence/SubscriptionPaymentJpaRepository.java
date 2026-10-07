package com.hrcoresystem.hrcore_api.modules.platform.billing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionPaymentJpaRepository extends JpaRepository<SubscriptionPaymentEntity, UUID> {

    Optional<SubscriptionPaymentEntity> findByStripeInvoiceId(String stripeInvoiceId);

    List<SubscriptionPaymentEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
