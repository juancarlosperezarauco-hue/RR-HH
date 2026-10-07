package com.hrcoresystem.hrcore_api.modules.platform.subscriptions.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscription;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.repository.PlatformSubscriptionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PlatformSubscriptionRepositoryAdapter implements PlatformSubscriptionRepository {

    private final PlatformSubscriptionJpaRepository jpaRepository;

    public PlatformSubscriptionRepositoryAdapter(PlatformSubscriptionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PlatformSubscription save(PlatformSubscription subscription) {
        PlatformSubscriptionEntity entity = toEntity(subscription);
        PlatformSubscriptionEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<PlatformSubscription> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<PlatformSubscription> findByStripeSubscriptionId(String stripeSubscriptionId) {
        return jpaRepository.findByStripeSubscriptionId(stripeSubscriptionId).map(this::toDomain);
    }

    @Override
    public Optional<PlatformSubscription> findCurrentByTenantId(UUID tenantId) {
        return jpaRepository.findByTenantIdAndCurrentSubscriptionTrue(tenantId).map(this::toDomain);
    }

    @Override
    public List<PlatformSubscription> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void clearCurrentSubscriptions(UUID tenantId) {
        jpaRepository.clearCurrentSubscriptions(tenantId);
    }

    private PlatformSubscriptionEntity toEntity(PlatformSubscription subscription) {
        PlatformSubscriptionEntity entity = new PlatformSubscriptionEntity();
        entity.setId(subscription.id());
        entity.setTenantId(subscription.tenantId());
        entity.setPlanId(subscription.planId());
        entity.setStatus(subscription.status());
        entity.setTrial(subscription.trial());
        entity.setCurrentSubscription(subscription.currentSubscription());
        entity.setStripeSubscriptionId(subscription.stripeSubscriptionId());
        entity.setStripePriceId(subscription.stripePriceId());
        entity.setBillingInterval(subscription.billingInterval());
        entity.setStartedAt(subscription.startedAt());
        entity.setExpiresAt(subscription.expiresAt());
        entity.setCurrentPeriodStart(subscription.currentPeriodStart());
        entity.setCurrentPeriodEnd(subscription.currentPeriodEnd());
        entity.setCancelAtPeriodEnd(subscription.cancelAtPeriodEnd());
        entity.setCancelledAt(subscription.cancelledAt());
        return entity;
    }

    private PlatformSubscription toDomain(PlatformSubscriptionEntity entity) {
        return new PlatformSubscription(
                entity.getId(),
                entity.getTenantId(),
                entity.getPlanId(),
                entity.getStatus(),
                entity.isTrial(),
                entity.isCurrentSubscription(),
                entity.getStripeSubscriptionId(),
                entity.getStripePriceId(),
                entity.getBillingInterval(),
                entity.getStartedAt(),
                entity.getExpiresAt(),
                entity.getCurrentPeriodStart(),
                entity.getCurrentPeriodEnd(),
                entity.isCancelAtPeriodEnd(),
                entity.getCancelledAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
