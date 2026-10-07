package com.hrcoresystem.hrcore_api.modules.platform.billing.application.service;

import com.hrcoresystem.hrcore_api.modules.platform.billing.application.config.StripeBillingProperties;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.exception.BillingException;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.BillingInterval;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
public class StripeCheckoutService {

    private final StripeBillingProperties properties;

    public StripeCheckoutService(StripeBillingProperties properties) {
        this.properties = properties;
    }

    public Session createSubscriptionCheckoutSession(
            PlatformTenant tenant,
            PlatformPlan plan,
            BillingInterval billingInterval
    ) {
        validateCheckoutConfiguration();

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(tenant.stripeCustomerId())
                    .setSuccessUrl(properties.getSuccessUrl())
                    .setCancelUrl(properties.getCancelUrl())
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(normalizeCurrency(plan.currency()))
                                                    .setUnitAmount(toStripeMinorUnits(resolveAmount(plan, billingInterval)))
                                                    .setRecurring(
                                                            SessionCreateParams.LineItem.PriceData.Recurring.builder()
                                                                    .setInterval(
                                                                            billingInterval == BillingInterval.MONTHLY
                                                                                    ? SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH
                                                                                    : SessionCreateParams.LineItem.PriceData.Recurring.Interval.YEAR
                                                                    )
                                                                    .setIntervalCount(1L)
                                                                    .build()
                                                    )
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(plan.name())
                                                                    .setDescription(plan.description())
                                                                    .putMetadata("plan_id", plan.id().toString())
                                                                    .putMetadata("plan_code", plan.code())
                                                                    .putMetadata("billing_interval", billingInterval.name())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .setQuantity(1L)
                                    .build()
                    )
                    .putMetadata("tenant_id", tenant.id().toString())
                    .putMetadata("tenant_slug", tenant.slug())
                    .putMetadata("plan_id", plan.id().toString())
                    .putMetadata("plan_code", plan.code())
                    .putMetadata("billing_interval", billingInterval.name())
                    .setSubscriptionData(
                            SessionCreateParams.SubscriptionData.builder()
                                    .putMetadata("tenant_id", tenant.id().toString())
                                    .putMetadata("tenant_slug", tenant.slug())
                                    .putMetadata("plan_id", plan.id().toString())
                                    .putMetadata("plan_code", plan.code())
                                    .putMetadata("billing_interval", billingInterval.name())
                                    .build()
                    )
                    .build();

            return Session.create(params);
        } catch (StripeException exception) {
            throw new BillingException("Could not create Stripe checkout session", exception);
        }
    }

    private void validateCheckoutConfiguration() {
        if (!StringUtils.hasText(properties.getSuccessUrl())) {
            throw new BillingException("Stripe success URL is not configured");
        }

        if (!StringUtils.hasText(properties.getCancelUrl())) {
            throw new BillingException("Stripe cancel URL is not configured");
        }
    }

    private BigDecimal resolveAmount(PlatformPlan plan, BillingInterval billingInterval) {
        BigDecimal amount = billingInterval == BillingInterval.MONTHLY
                ? plan.monthlyAmount()
                : plan.yearlyAmount();

        if (amount == null) {
            throw new BillingException("Selected plan does not have an amount configured for " + billingInterval.name());
        }

        return amount;
    }

    private long toStripeMinorUnits(BigDecimal amount) {
        return amount
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .longValueExact();
    }

    private String normalizeCurrency(String currency) {
        if (!StringUtils.hasText(currency)) {
            return "usd";
        }

        return currency.trim().toLowerCase();
    }
}
