package com.hrcoresystem.hrcore_api.modules.platform.billing.application.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class StripeBillingConfig {

    private final StripeBillingProperties properties;

    public StripeBillingConfig(StripeBillingProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void configureStripe() {
        if (!StringUtils.hasText(properties.getSecretKey())) {
            return;
        }

        Stripe.apiKey = properties.getSecretKey().trim();
    }
}
