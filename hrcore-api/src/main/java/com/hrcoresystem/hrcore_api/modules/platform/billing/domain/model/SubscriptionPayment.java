package com.hrcoresystem.hrcore_api.modules.platform.billing.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SubscriptionPayment(
        UUID id,
        UUID tenantId,
        UUID platformSubscriptionId,
        UUID planId,
        String stripeCustomerId,
        String stripeSubscriptionId,
        String stripeInvoiceId,
        String stripePaymentIntentId,
        String stripeChargeId,
        String invoiceNumber,
        String hostedInvoiceUrl,
        String invoicePdfUrl,
        BigDecimal amount,
        String currency,
        String status,
        String billingReason,
        Instant paidAt,
        Instant failedAt,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {
}
