package com.hrcoresystem.hrcore_api.modules.platform.billing.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.model.StripeWebhookEvent;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.repository.StripeWebhookEventRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class StripeWebhookEventRepositoryAdapter implements StripeWebhookEventRepository {

    private final StripeWebhookEventJpaRepository jpaRepository;

    public StripeWebhookEventRepositoryAdapter(
            StripeWebhookEventJpaRepository jpaRepository
    ) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public StripeWebhookEvent save(StripeWebhookEvent event) {
        return toDomain(jpaRepository.save(toEntity(event)));
    }

    @Override
    public Optional<StripeWebhookEvent> findByStripeEventId(String stripeEventId) {
        return jpaRepository.findByStripeEventId(stripeEventId).map(this::toDomain);
    }

    @Override
    public boolean existsByStripeEventId(String stripeEventId) {
        return jpaRepository.existsByStripeEventId(stripeEventId);
    }

    private StripeWebhookEventEntity toEntity(StripeWebhookEvent event) {
        StripeWebhookEventEntity entity = new StripeWebhookEventEntity();
        entity.setId(event.id());
        entity.setStripeEventId(event.stripeEventId());
        entity.setEventType(event.eventType());
        entity.setProcessed(event.processed());
        entity.setProcessedAt(event.processedAt());
        entity.setProcessingAttempts(event.processingAttempts());
        entity.setLastError(event.lastError());
        entity.setPayload(event.payload());
        return entity;
    }

    private StripeWebhookEvent toDomain(StripeWebhookEventEntity entity) {
        return new StripeWebhookEvent(
                entity.getId(),
                entity.getStripeEventId(),
                entity.getEventType(),
                entity.isProcessed(),
                entity.getProcessedAt(),
                entity.getProcessingAttempts(),
                entity.getLastError(),
                entity.getPayload(),
                entity.getReceivedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
