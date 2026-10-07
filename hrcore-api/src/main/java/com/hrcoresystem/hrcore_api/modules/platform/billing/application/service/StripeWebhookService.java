package com.hrcoresystem.hrcore_api.modules.platform.billing.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.config.StripeBillingProperties;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.exception.BillingException;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.model.StripeWebhookEvent;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.repository.StripeWebhookEventRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service
public class StripeWebhookService {

    private final StripeBillingProperties properties;
    private final StripeWebhookEventRepository stripeWebhookEventRepository;
    private final StripeSubscriptionSyncService stripeSubscriptionSyncService;
    private final AuditTrailService auditTrailService;
    private final ObjectMapper objectMapper;

    public StripeWebhookService(
            StripeBillingProperties properties,
            StripeWebhookEventRepository stripeWebhookEventRepository,
            StripeSubscriptionSyncService stripeSubscriptionSyncService,
            AuditTrailService auditTrailService,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.stripeWebhookEventRepository = stripeWebhookEventRepository;
        this.stripeSubscriptionSyncService = stripeSubscriptionSyncService;
        this.auditTrailService = auditTrailService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void handle(String payload, String signature) {
        Event event = constructEvent(payload, signature);

        if (stripeWebhookEventRepository.existsByStripeEventId(event.getId())) {
            return;
        }

        JsonNode payloadNode = readPayload(payload);

        StripeWebhookEvent savedEvent = stripeWebhookEventRepository.save(
                new StripeWebhookEvent(
                        null,
                        event.getId(),
                        event.getType(),
                        false,
                        null,
                        0,
                        null,
                        payloadNode,
                        Instant.now(),
                        null,
                        null
                )
        );

        auditTrailService.recordPlatformEvent(
                AuditEventTypes.STRIPE_WEBHOOK_RECEIVED,
                "STRIPE_WEBHOOK",
                event.getId(),
                event.getType()
        );

        try {
            switch (event.getType()) {
                case "checkout.session.completed" ->
                        stripeSubscriptionSyncService.handleCheckoutSessionCompleted(event);
                case "customer.subscription.created",
                     "customer.subscription.updated",
                     "customer.subscription.deleted" ->
                        stripeSubscriptionSyncService.handleSubscriptionChanged(event);
                case "invoice.payment_succeeded" ->
                        stripeSubscriptionSyncService.handleInvoicePaymentSucceeded(event);
                case "invoice.payment_failed" ->
                        stripeSubscriptionSyncService.handleInvoicePaymentFailed(event);
                default -> {
                    // Non-critical event for this domain; still mark processed for idempotency.
                }
            }

            stripeWebhookEventRepository.save(
                    new StripeWebhookEvent(
                            savedEvent.id(),
                            savedEvent.stripeEventId(),
                            savedEvent.eventType(),
                            true,
                            Instant.now(),
                            savedEvent.processingAttempts() + 1,
                            null,
                            savedEvent.payload(),
                            savedEvent.receivedAt(),
                            savedEvent.createdAt(),
                            savedEvent.updatedAt()
                    )
            );

            auditTrailService.recordPlatformEvent(
                    AuditEventTypes.STRIPE_WEBHOOK_PROCESSED,
                    "STRIPE_WEBHOOK",
                    event.getId(),
                    event.getType()
            );
        } catch (RuntimeException exception) {
            stripeWebhookEventRepository.save(
                    new StripeWebhookEvent(
                            savedEvent.id(),
                            savedEvent.stripeEventId(),
                            savedEvent.eventType(),
                            false,
                            null,
                            savedEvent.processingAttempts() + 1,
                            exception.getMessage(),
                            savedEvent.payload(),
                            savedEvent.receivedAt(),
                            savedEvent.createdAt(),
                            savedEvent.updatedAt()
                    )
            );

            throw exception;
        }
    }

    private Event constructEvent(String payload, String signature) {
        if (!StringUtils.hasText(properties.getWebhookSecret())) {
            throw new BillingException("Stripe webhook secret is not configured");
        }

        try {
            return Webhook.constructEvent(payload, signature, properties.getWebhookSecret());
        } catch (SignatureVerificationException exception) {
            throw new BillingException("Invalid Stripe webhook signature", exception);
        }
    }

    private JsonNode readPayload(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception exception) {
            return null;
        }
    }
}
