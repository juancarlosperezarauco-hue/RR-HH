package com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.platform.audit.PlatformAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.service.StripeCheckoutService;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.service.StripeCustomerService;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.exception.BillingException;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.model.SubscriptionCheckoutSession;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.repository.SubscriptionCheckoutSessionRepository;
import com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.dto.PublicPaidSignupRequest;
import com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.dto.PublicPaidSignupResponse;
import com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.service.TenantOwnerAdminProvisioningService;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.BillingInterval;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscription;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.repository.PlatformSubscriptionRepository;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto.CreateTenantRequest;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto.PlatformTenantResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.usecase.CreateTenantUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import com.stripe.model.checkout.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class PublicPaidSignupUseCase {

    private final CreateTenantUseCase createTenantUseCase;
    private final PlatformTenantRepository platformTenantRepository;
    private final PlatformSubscriptionRepository platformSubscriptionRepository;
    private final PlatformPlanRepository platformPlanRepository;
    private final TenantOwnerAdminProvisioningService tenantOwnerAdminProvisioningService;
    private final StripeCustomerService stripeCustomerService;
    private final StripeCheckoutService stripeCheckoutService;
    private final SubscriptionCheckoutSessionRepository checkoutSessionRepository;
    private final AuditTrailService auditTrailService;

    public PublicPaidSignupUseCase(
            CreateTenantUseCase createTenantUseCase,
            PlatformTenantRepository platformTenantRepository,
            PlatformSubscriptionRepository platformSubscriptionRepository,
            PlatformPlanRepository platformPlanRepository,
            TenantOwnerAdminProvisioningService tenantOwnerAdminProvisioningService,
            StripeCustomerService stripeCustomerService,
            StripeCheckoutService stripeCheckoutService,
            SubscriptionCheckoutSessionRepository checkoutSessionRepository,
            AuditTrailService auditTrailService
    ) {
        this.createTenantUseCase = createTenantUseCase;
        this.platformTenantRepository = platformTenantRepository;
        this.platformSubscriptionRepository = platformSubscriptionRepository;
        this.platformPlanRepository = platformPlanRepository;
        this.tenantOwnerAdminProvisioningService = tenantOwnerAdminProvisioningService;
        this.stripeCustomerService = stripeCustomerService;
        this.stripeCheckoutService = stripeCheckoutService;
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public PublicPaidSignupResponse execute(PublicPaidSignupRequest request) {
        PlatformPlan selectedPlan = platformPlanRepository.findByCode(request.planCode().trim().toUpperCase())
                .orElseThrow(() -> new BillingException("Billing plan not found: " + request.planCode()));

        BillingInterval billingInterval = parseBillingInterval(request.billingInterval());

        validateSelectedPlan(selectedPlan);

        PlatformTenantResponse createdTenantResponse = createTenantUseCase.execute(
                new CreateTenantRequest(
                        request.companyName().trim(),
                        request.tenantSlug().trim(),
                        "DEMO"
                )
        );

        PlatformTenant createdTenant = platformTenantRepository.findById(createdTenantResponse.id())
                .orElseThrow();

        tenantOwnerAdminProvisioningService.provisionOwnerAdmin(
                createdTenant.schemaName(),
                createdTenant.slug(),
                request.adminEmail().trim().toLowerCase(),
                request.password(),
                normalizeName(request.firstName()),
                normalizeName(request.lastName())
        );

        PlatformSubscription currentSubscription = platformSubscriptionRepository.findCurrentByTenantId(createdTenant.id())
                .orElseThrow();

        PlatformPlan initialPlan = platformPlanRepository.findById(currentSubscription.planId())
                .orElseThrow();

        PlatformTenant tenantWithCustomer = stripeCustomerService.ensureStripeCustomer(
                createdTenant,
                request.adminEmail()
        );

        Session stripeSession = stripeCheckoutService.createSubscriptionCheckoutSession(
                tenantWithCustomer,
                selectedPlan,
                billingInterval
        );

        SubscriptionCheckoutSession savedCheckout = checkoutSessionRepository.save(
                new SubscriptionCheckoutSession(
                        null,
                        tenantWithCustomer.id(),
                        selectedPlan.id(),
                        null,
                        request.adminEmail().trim().toLowerCase(),
                        billingInterval,
                        normalizeCheckoutStatus(stripeSession.getStatus()),
                        tenantWithCustomer.stripeCustomerId(),
                        stripeSession.getId(),
                        null,
                        null,
                        stripeSession.getUrl(),
                        null,
                        null,
                        resolveAmount(selectedPlan, billingInterval),
                        selectedPlan.currency(),
                        null,
                        stripeSession.getExpiresAt() == null ? null : Instant.ofEpochSecond(stripeSession.getExpiresAt()),
                        null,
                        null
                )
        );

        auditTrailService.recordPlatformEvent(
                AuditEventTypes.PUBLIC_SIGNUP_CHECKOUT_CREATED,
                "TENANT",
                tenantWithCustomer.id().toString(),
                PlatformAuditPayloads.details(
                        "tenantSlug", tenantWithCustomer.slug(),
                        "adminEmail", request.adminEmail().trim().toLowerCase(),
                        "initialPlanCode", initialPlan.code(),
                        "selectedPlanCode", selectedPlan.code(),
                        "billingInterval", billingInterval.name(),
                        "checkoutSessionId", stripeSession.getId()
                ),
                null,
                PlatformAuditPayloads.tenantState(tenantWithCustomer)
        );

        return new PublicPaidSignupResponse(
                tenantWithCustomer.id(),
                tenantWithCustomer.slug(),
                tenantWithCustomer.name(),
                request.adminEmail().trim().toLowerCase(),
                "OWNER_ADMIN",
                initialPlan.code(),
                selectedPlan.code(),
                billingInterval.name(),
                savedCheckout.stripeSessionId(),
                savedCheckout.checkoutUrl(),
                savedCheckout.status(),
                savedCheckout.expiresAt(),
                "Redirect the user to checkoutUrl. The subscription will be activated after Stripe webhook confirmation and the owner must verify the inbox before signing in."
        );
    }

    private void validateSelectedPlan(PlatformPlan plan) {
        if (!plan.active()) {
            throw new BillingException("Selected plan is not active");
        }

        if (!plan.publicVisible()) {
            throw new BillingException("Selected plan is not available for public signup");
        }

        if (!"PAID".equalsIgnoreCase(plan.planType())) {
            throw new BillingException("Only paid plans can be used for paid signup");
        }
    }

    private BillingInterval parseBillingInterval(String value) {
        try {
            return BillingInterval.valueOf(value.trim().toUpperCase());
        } catch (Exception exception) {
            throw new BillingException("Invalid billing interval. Allowed values: MONTHLY, YEARLY");
        }
    }

    private String normalizeCheckoutStatus(String stripeStatus) {
        if (!StringUtils.hasText(stripeStatus)) {
            return "PENDING";
        }

        return switch (stripeStatus.trim().toLowerCase()) {
            case "open" -> "OPEN";
            case "complete" -> "COMPLETED";
            case "expired" -> "EXPIRED";
            default -> "PENDING";
        };
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

    private String normalizeName(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        return value.trim();
    }
}
