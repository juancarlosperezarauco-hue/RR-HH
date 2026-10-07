package com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto;

public record BillingConfigurationStatusResponse(
        boolean stripeSecretKeyConfigured,
        boolean stripeWebhookSecretConfigured,
        boolean successUrlConfigured,
        boolean cancelUrlConfigured,
        boolean customerPortalReturnUrlConfigured,
        boolean readyForCheckout,
        boolean readyForWebhook,
        boolean readyForCustomerPortal
) {
}
