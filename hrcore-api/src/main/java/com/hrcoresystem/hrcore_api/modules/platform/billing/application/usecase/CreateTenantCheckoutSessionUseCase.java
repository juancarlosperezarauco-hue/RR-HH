package com.hrcoresystem.hrcore_api.modules.platform.billing.application.usecase;

import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto.CheckoutSessionResponse;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto.CreateCheckoutSessionRequest;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.service.StripeCheckoutService;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.service.StripeCustomerService;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.exception.BillingException;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.model.SubscriptionCheckoutSession;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.repository.SubscriptionCheckoutSessionRepository;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.BillingInterval;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.exception.PlatformTenantNotFoundException;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import com.stripe.model.checkout.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class CreateTenantCheckoutSessionUseCase {

    private final PlatformTenantRepository platformTenantRepository;
    private final PlatformPlanRepository platformPlanRepository;
    private final SubscriptionCheckoutSessionRepository checkoutSessionRepository;
    private final StripeCustomerService stripeCustomerService;
    private final StripeCheckoutService stripeCheckoutService;
    private final SecurityContextFacade securityContextFacade;

    public CreateTenantCheckoutSessionUseCase(
            PlatformTenantRepository platformTenantRepository,
            PlatformPlanRepository platformPlanRepository,
            SubscriptionCheckoutSessionRepository checkoutSessionRepository,
            StripeCustomerService stripeCustomerService,
            StripeCheckoutService stripeCheckoutService,
            SecurityContextFacade securityContextFacade
    ) {
        this.platformTenantRepository = platformTenantRepository;
        this.platformPlanRepository = platformPlanRepository;
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.stripeCustomerService = stripeCustomerService;
        this.stripeCheckoutService = stripeCheckoutService;
        this.securityContextFacade = securityContextFacade;
    }

    @Transactional
    public CheckoutSessionResponse execute(CreateCheckoutSessionRequest request) {
        String tenantSlug = TenantContextHolder.getRequired().tenantSlug();

        PlatformTenant tenant = platformTenantRepository.findBySlug(tenantSlug)
                .orElseThrow(() -> new PlatformTenantNotFoundException("Tenant not found with slug: " + tenantSlug));

        PlatformPlan plan = platformPlanRepository.findByCode(request.planCode().trim().toUpperCase())
                .orElseThrow(() -> new BillingException("Billing plan not found: " + request.planCode()));

        BillingInterval billingInterval = parseBillingInterval(request.billingInterval());

        validatePlanForCheckout(plan);

        PlatformTenant tenantWithCustomer = stripeCustomerService.ensureStripeCustomer(
                tenant,
                securityContextFacade.getCurrentEmail()
        );

        Session stripeSession = stripeCheckoutService.createSubscriptionCheckoutSession(
                tenantWithCustomer,
                plan,
                billingInterval
        );

        SubscriptionCheckoutSession saved = checkoutSessionRepository.save(
                new SubscriptionCheckoutSession(
                        null,
                        tenantWithCustomer.id(),
                        plan.id(),
                        parseCurrentUserId(),
                        securityContextFacade.getCurrentEmail(),
                        billingInterval,
                        normalizeCheckoutStatus(stripeSession.getStatus()),
                        tenantWithCustomer.stripeCustomerId(),
                        stripeSession.getId(),
                        null,
                        null,
                        stripeSession.getUrl(),
                        null,
                        null,
                        resolveAmount(plan, billingInterval),
                        plan.currency(),
                        null,
                        stripeSession.getExpiresAt() == null ? null : Instant.ofEpochSecond(stripeSession.getExpiresAt()),
                        null,
                        null
                )
        );

        return new CheckoutSessionResponse(
                saved.id(),
                saved.stripeSessionId(),
                saved.checkoutUrl(),
                saved.status(),
                plan.code(),
                billingInterval.name(),
                saved.expiresAt()
        );
    }

    private void validatePlanForCheckout(PlatformPlan plan) {
        if (!plan.active()) {
            throw new BillingException("Selected plan is not active");
        }

        if (!plan.publicVisible()) {
            throw new BillingException("Selected plan is not available for checkout");
        }

        if ("DEMO".equalsIgnoreCase(plan.planType())) {
            throw new BillingException("Demo plan cannot be purchased");
        }

        if ("ENTERPRISE".equalsIgnoreCase(plan.planType())) {
            throw new BillingException("Enterprise plan requires commercial contact");
        }
    }

    private BillingInterval parseBillingInterval(String value) {
        try {
            return BillingInterval.valueOf(value.trim().toUpperCase());
        } catch (Exception exception) {
            throw new BillingException("Invalid billing interval. Allowed values: MONTHLY, YEARLY");
        }
    }

    private UUID parseCurrentUserId() {
        String subject = securityContextFacade.getCurrentSubject();

        if (subject == null || subject.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(subject.trim());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private BigDecimal resolveAmount(PlatformPlan plan, BillingInterval billingInterval) {
        return billingInterval == BillingInterval.MONTHLY
                ? plan.monthlyAmount()
                : plan.yearlyAmount();
    }

    private String normalizeCheckoutStatus(String stripeStatus) {
        if (stripeStatus == null || stripeStatus.isBlank()) {
            return "PENDING";
        }

        return switch (stripeStatus.trim().toLowerCase()) {
            case "open" -> "OPEN";
            case "complete" -> "COMPLETED";
            case "expired" -> "EXPIRED";
            default -> "PENDING";
        };
    }
}
