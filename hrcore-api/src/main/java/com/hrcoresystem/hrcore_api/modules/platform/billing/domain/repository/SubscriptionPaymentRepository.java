package com.hrcoresystem.hrcore_api.modules.platform.billing.domain.repository;

import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.model.SubscriptionPayment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionPaymentRepository {

    SubscriptionPayment save(SubscriptionPayment payment);

    Optional<SubscriptionPayment> findByStripeInvoiceId(String stripeInvoiceId);

    List<SubscriptionPayment> findByTenantId(UUID tenantId);
}
