package com.hrcoresystem.hrcore_api.modules.platform.billing.domain.repository;

import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.model.StripeWebhookEvent;

import java.util.Optional;

public interface StripeWebhookEventRepository {

    StripeWebhookEvent save(StripeWebhookEvent event);

    Optional<StripeWebhookEvent> findByStripeEventId(String stripeEventId);

    boolean existsByStripeEventId(String stripeEventId);
}
