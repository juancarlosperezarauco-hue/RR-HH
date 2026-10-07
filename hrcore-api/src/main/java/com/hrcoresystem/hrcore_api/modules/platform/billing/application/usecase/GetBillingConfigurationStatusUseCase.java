package com.hrcoresystem.hrcore_api.modules.platform.billing.application.usecase;

import com.hrcoresystem.hrcore_api.modules.platform.billing.application.config.StripeBillingProperties;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto.BillingConfigurationStatusResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GetBillingConfigurationStatusUseCase {

    private final StripeBillingProperties properties;

    public GetBillingConfigurationStatusUseCase(StripeBillingProperties properties) {
        this.properties = properties;
    }

    public BillingConfigurationStatusResponse execute() {
        boolean secret = StringUtils.hasText(properties.getSecretKey());
        boolean webhook = StringUtils.hasText(properties.getWebhookSecret());
        boolean success = StringUtils.hasText(properties.getSuccessUrl());
        boolean cancel = StringUtils.hasText(properties.getCancelUrl());
        boolean portal = StringUtils.hasText(properties.getCustomerPortalReturnUrl());

        return new BillingConfigurationStatusResponse(
                secret,
                webhook,
                success,
                cancel,
                portal,
                secret && success && cancel,
                secret && webhook,
                secret && portal
        );
    }
}
