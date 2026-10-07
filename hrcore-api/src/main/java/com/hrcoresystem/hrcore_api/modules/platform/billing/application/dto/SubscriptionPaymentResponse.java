package com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SubscriptionPaymentResponse(
        UUID id,
        String tenantSlug,
        String planCode,
        String stripeInvoiceId,
        String stripeSubscriptionId,
        String invoiceNumber,
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
