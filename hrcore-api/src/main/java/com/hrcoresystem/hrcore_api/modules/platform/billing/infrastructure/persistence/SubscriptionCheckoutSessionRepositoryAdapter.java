package com.hrcoresystem.hrcore_api.modules.platform.billing.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.model.SubscriptionCheckoutSession;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.repository.SubscriptionCheckoutSessionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SubscriptionCheckoutSessionRepositoryAdapter implements SubscriptionCheckoutSessionRepository {

    private final SubscriptionCheckoutSessionJpaRepository jpaRepository;

    public SubscriptionCheckoutSessionRepositoryAdapter(SubscriptionCheckoutSessionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public SubscriptionCheckoutSession save(SubscriptionCheckoutSession session) {
        return toDomain(jpaRepository.save(toEntity(session)));
    }

    @Override
    public Optional<SubscriptionCheckoutSession> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<SubscriptionCheckoutSession> findByStripeSessionId(String stripeSessionId) {
        return jpaRepository.findByStripeSessionId(stripeSessionId).map(this::toDomain);
    }

    @Override
    public Optional<SubscriptionCheckoutSession> findByStripeSubscriptionId(String stripeSubscriptionId) {
        return jpaRepository.findByStripeSubscriptionId(stripeSubscriptionId).map(this::toDomain);
    }

    @Override
    public Optional<UUID> findByTenantIdFromStripeSubscription(String stripeSubscriptionId) {
        return jpaRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .map(SubscriptionCheckoutSessionEntity::getTenantId);
    }

    @Override
    public List<SubscriptionCheckoutSession> findByTenantId(UUID tenantId) {
        return jpaRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<SubscriptionCheckoutSession> findPendingActivationCandidates() {
        return jpaRepository.findPendingActivationCandidates()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private SubscriptionCheckoutSessionEntity toEntity(SubscriptionCheckoutSession session) {
        SubscriptionCheckoutSessionEntity entity = new SubscriptionCheckoutSessionEntity();
        entity.setId(session.id());
        entity.setTenantId(session.tenantId());
        entity.setPlanId(session.planId());
        entity.setRequestedByUserId(session.requestedByUserId());
        entity.setRequestedByEmail(session.requestedByEmail());
        entity.setBillingInterval(session.billingInterval());
        entity.setStatus(session.status());
        entity.setStripeCustomerId(session.stripeCustomerId());
        entity.setStripeSessionId(session.stripeSessionId());
        entity.setStripeSubscriptionId(session.stripeSubscriptionId());
        entity.setStripePriceId(session.stripePriceId());
        entity.setCheckoutUrl(session.checkoutUrl());
        entity.setSuccessUrl(session.successUrl());
        entity.setCancelUrl(session.cancelUrl());
        entity.setAmount(session.amount());
        entity.setCurrency(session.currency());
        entity.setCompletedAt(session.completedAt());
        entity.setExpiresAt(session.expiresAt());
        return entity;
    }

    private SubscriptionCheckoutSession toDomain(SubscriptionCheckoutSessionEntity entity) {
        return new SubscriptionCheckoutSession(
                entity.getId(),
                entity.getTenantId(),
                entity.getPlanId(),
                entity.getRequestedByUserId(),
                entity.getRequestedByEmail(),
                entity.getBillingInterval(),
                entity.getStatus(),
                entity.getStripeCustomerId(),
                entity.getStripeSessionId(),
                entity.getStripeSubscriptionId(),
                entity.getStripePriceId(),
                entity.getCheckoutUrl(),
                entity.getSuccessUrl(),
                entity.getCancelUrl(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getCompletedAt(),
                entity.getExpiresAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
