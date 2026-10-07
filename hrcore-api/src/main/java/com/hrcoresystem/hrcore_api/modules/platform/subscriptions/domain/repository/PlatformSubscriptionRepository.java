package com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.repository;

import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlatformSubscriptionRepository {

    PlatformSubscription save(PlatformSubscription subscription);

    Optional<PlatformSubscription> findById(UUID id);

    Optional<PlatformSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    Optional<PlatformSubscription> findCurrentByTenantId(UUID tenantId);

    List<PlatformSubscription> findAll();

    void clearCurrentSubscriptions(UUID tenantId);
}
