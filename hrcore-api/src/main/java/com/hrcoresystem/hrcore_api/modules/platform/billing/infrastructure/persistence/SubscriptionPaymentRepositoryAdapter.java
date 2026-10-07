package com.hrcoresystem.hrcore_api.modules.platform.billing.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.model.SubscriptionPayment;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.repository.SubscriptionPaymentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SubscriptionPaymentRepositoryAdapter implements SubscriptionPaymentRepository {

    private final SubscriptionPaymentJpaRepository jpaRepository;

    public SubscriptionPaymentRepositoryAdapter(SubscriptionPaymentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public SubscriptionPayment save(SubscriptionPayment payment) {
        return toDomain(jpaRepository.save(toEntity(payment)));
    }

    @Override
    public Optional<SubscriptionPayment> findByStripeInvoiceId(String stripeInvoiceId) {
        return jpaRepository.findByStripeInvoiceId(stripeInvoiceId).map(this::toDomain);
    }

    @Override
    public List<SubscriptionPayment> findByTenantId(UUID tenantId) {
        return jpaRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private SubscriptionPaymentEntity toEntity(SubscriptionPayment payment) {
        SubscriptionPaymentEntity entity = new SubscriptionPaymentEntity();
        entity.setId(payment.id());
        entity.setTenantId(payment.tenantId());
        entity.setPlatformSubscriptionId(payment.platformSubscriptionId());
        entity.setPlanId(payment.planId());
        entity.setStripeCustomerId(payment.stripeCustomerId());
        entity.setStripeSubscriptionId(payment.stripeSubscriptionId());
        entity.setStripeInvoiceId(payment.stripeInvoiceId());
        entity.setStripePaymentIntentId(payment.stripePaymentIntentId());
        entity.setStripeChargeId(payment.stripeChargeId());
        entity.setInvoiceNumber(payment.invoiceNumber());
        entity.setHostedInvoiceUrl(payment.hostedInvoiceUrl());
        entity.setInvoicePdfUrl(payment.invoicePdfUrl());
        entity.setAmount(payment.amount());
        entity.setCurrency(payment.currency());
        entity.setStatus(payment.status());
        entity.setBillingReason(payment.billingReason());
        entity.setPaidAt(payment.paidAt());
        entity.setFailedAt(payment.failedAt());
        entity.setFailureReason(payment.failureReason());
        return entity;
    }

    private SubscriptionPayment toDomain(SubscriptionPaymentEntity entity) {
        return new SubscriptionPayment(
                entity.getId(),
                entity.getTenantId(),
                entity.getPlatformSubscriptionId(),
                entity.getPlanId(),
                entity.getStripeCustomerId(),
                entity.getStripeSubscriptionId(),
                entity.getStripeInvoiceId(),
                entity.getStripePaymentIntentId(),
                entity.getStripeChargeId(),
                entity.getInvoiceNumber(),
                entity.getHostedInvoiceUrl(),
                entity.getInvoicePdfUrl(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getStatus(),
                entity.getBillingReason(),
                entity.getPaidAt(),
                entity.getFailedAt(),
                entity.getFailureReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
