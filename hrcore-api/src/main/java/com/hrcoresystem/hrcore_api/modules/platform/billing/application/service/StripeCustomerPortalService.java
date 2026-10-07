package com.hrcoresystem.hrcore_api.modules.platform.billing.application.service;

import com.hrcoresystem.hrcore_api.modules.platform.billing.application.config.StripeBillingProperties;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.exception.BillingException;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.stripe.exception.StripeException;
import com.stripe.model.billingportal.Session;
import com.stripe.param.billingportal.SessionCreateParams;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class StripeCustomerPortalService {

    private final StripeBillingProperties properties;

    public StripeCustomerPortalService(StripeBillingProperties properties) {
        this.properties = properties;
    }

    public Session createPortalSession(PlatformTenant tenant) {
        if (!StringUtils.hasText(tenant.stripeCustomerId())) {
            throw new BillingException("Tenant does not have a Stripe customer yet");
        }

        if (!StringUtils.hasText(properties.getCustomerPortalReturnUrl())) {
            throw new BillingException("Stripe customer portal return URL is not configured");
        }

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setCustomer(tenant.stripeCustomerId())
                    .setReturnUrl(properties.getCustomerPortalReturnUrl())
                    .build();

            return Session.create(params);
        } catch (StripeException exception) {
            throw new BillingException("Could not create Stripe customer portal session", exception);
        }
    }
}
