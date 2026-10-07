package com.hrcoresystem.hrcore_api.modules.platform.billing.application.service;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.exception.BillingException;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.model.SubscriptionCheckoutSession;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.repository.SubscriptionCheckoutSessionRepository;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.PlatformSubscriptionProvisioningService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscription;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscriptionStatus;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.repository.PlatformSubscriptionRepository;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

@Service
public class StripeCheckoutReconciliationService {

    private static final Logger logger = LoggerFactory.getLogger(StripeCheckoutReconciliationService.class);

    private final SubscriptionCheckoutSessionRepository checkoutSessionRepository;
    private final PlatformTenantRepository platformTenantRepository;
    private final PlatformPlanRepository platformPlanRepository;
    private final PlatformSubscriptionRepository platformSubscriptionRepository;
    private final PlatformSubscriptionProvisioningService subscriptionProvisioningService;
    private final AuditTrailService auditTrailService;

    public StripeCheckoutReconciliationService(
            SubscriptionCheckoutSessionRepository checkoutSessionRepository,
            PlatformTenantRepository platformTenantRepository,
            PlatformPlanRepository platformPlanRepository,
            PlatformSubscriptionRepository platformSubscriptionRepository,
            PlatformSubscriptionProvisioningService subscriptionProvisioningService,
            AuditTrailService auditTrailService
    ) {
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.platformTenantRepository = platformTenantRepository;
        this.platformPlanRepository = platformPlanRepository;
        this.platformSubscriptionRepository = platformSubscriptionRepository;
        this.subscriptionProvisioningService = subscriptionProvisioningService;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public ReconciliationResult reconcileByStripeSessionId(String stripeSessionId) {
        SubscriptionCheckoutSession localSession = checkoutSessionRepository.findByStripeSessionId(stripeSessionId)
                .orElseThrow(() -> new BillingException("Checkout session not found: " + stripeSessionId));

        PlatformTenant tenant = platformTenantRepository.findById(localSession.tenantId())
                .orElseThrow(() -> new BillingException("Tenant not found for checkout session"));

        PlatformPlan plan = platformPlanRepository.findById(localSession.planId())
                .orElseThrow(() -> new BillingException("Plan not found for checkout session"));

        if (isAlreadyActive(tenant.id(), plan.id())) {
            return ReconciliationResult.active(localSession, tenant, plan, "Plan already active.");
        }

        Session stripeSession = retrieveStripeSession(stripeSessionId);

        boolean paid = "paid".equalsIgnoreCase(stripeSession.getPaymentStatus());
        boolean complete = "complete".equalsIgnoreCase(stripeSession.getStatus());

        if (paid && complete) {
            logger.info(
                    "[STRIPE_CHECKOUT_RECONCILE_ACTIVE] tenantSlug={} planCode={} stripeSessionId={}",
                    tenant.slug(),
                    plan.code(),
                    stripeSessionId
            );

            subscriptionProvisioningService.assignCurrentSubscription(
                    tenant.id(),
                    plan.code(),
                    null,
                    true
            );

            SubscriptionCheckoutSession updatedSession = checkoutSessionRepository.save(
                    new SubscriptionCheckoutSession(
                            localSession.id(),
                            localSession.tenantId(),
                            localSession.planId(),
                            localSession.requestedByUserId(),
                            localSession.requestedByEmail(),
                            localSession.billingInterval(),
                            "COMPLETED",
                            localSession.stripeCustomerId(),
                            localSession.stripeSessionId(),
                            stripeSubscriptionId(stripeSession, localSession.stripeSubscriptionId()),
                            localSession.stripePriceId(),
                            localSession.checkoutUrl(),
                            localSession.successUrl(),
                            localSession.cancelUrl(),
                            localSession.amount(),
                            localSession.currency(),
                            Instant.now(),
                            localSession.expiresAt(),
                            localSession.createdAt(),
                            localSession.updatedAt()
                    )
            );

            return ReconciliationResult.active(updatedSession, tenant, plan, "Payment confirmed and plan activated.");
        }

        if ("expired".equalsIgnoreCase(stripeSession.getStatus())) {
            SubscriptionCheckoutSession updatedSession = checkoutSessionRepository.save(
                    new SubscriptionCheckoutSession(
                            localSession.id(),
                            localSession.tenantId(),
                            localSession.planId(),
                            localSession.requestedByUserId(),
                            localSession.requestedByEmail(),
                            localSession.billingInterval(),
                            "EXPIRED",
                            localSession.stripeCustomerId(),
                            localSession.stripeSessionId(),
                            stripeSubscriptionId(stripeSession, localSession.stripeSubscriptionId()),
                            localSession.stripePriceId(),
                            localSession.checkoutUrl(),
                            localSession.successUrl(),
                            localSession.cancelUrl(),
                            localSession.amount(),
                            localSession.currency(),
                            localSession.completedAt(),
                            localSession.expiresAt(),
                            localSession.createdAt(),
                            localSession.updatedAt()
                    )
            );

            return ReconciliationResult.failed(updatedSession, tenant, plan, "Checkout session expired.");
        }

        return ReconciliationResult.pending(localSession, tenant, plan, "Payment received by UI, activation is still pending.");
    }

    @Transactional
    public void reconcilePendingSessions() {
        checkoutSessionRepository.findPendingActivationCandidates()
                .forEach(session -> {
                    try {
                        reconcileByStripeSessionId(session.stripeSessionId());
                    } catch (Exception exception) {
                        logger.warn(
                                "[STRIPE_CHECKOUT_RECONCILE_JOB_FAILED] checkoutSessionId={} stripeSessionId={} error={}",
                                session.id(),
                                session.stripeSessionId(),
                                exception.getMessage()
                        );
                    }
                });
    }

    private boolean isAlreadyActive(UUID tenantId, UUID planId) {
        return platformSubscriptionRepository.findCurrentByTenantId(tenantId)
                .filter(subscription -> subscription.planId().equals(planId))
                .filter(subscription -> subscription.status() == PlatformSubscriptionStatus.ACTIVE)
                .isPresent();
    }

    private Session retrieveStripeSession(String stripeSessionId) {
        try {
            return Session.retrieve(stripeSessionId);
        } catch (StripeException exception) {
            throw new BillingException("Could not retrieve Stripe checkout session", exception);
        }
    }

    private String stripeSubscriptionId(Session stripeSession, String fallback) {
        if (stripeSession.getSubscription() != null && !stripeSession.getSubscription().isBlank()) {
            return stripeSession.getSubscription();
        }
        return fallback;
    }

    public record ReconciliationResult(
            String activationStatus,
            boolean paid,
            boolean active,
            boolean failed,
            boolean pending,
            SubscriptionCheckoutSession checkoutSession,
            PlatformTenant tenant,
            PlatformPlan plan,
            String message
    ) {
        public static ReconciliationResult active(
                SubscriptionCheckoutSession checkoutSession,
                PlatformTenant tenant,
                PlatformPlan plan,
                String message
        ) {
            return new ReconciliationResult(
                    "ACTIVE",
                    true,
                    true,
                    false,
                    false,
                    checkoutSession,
                    tenant,
                    plan,
                    message
            );
        }

        public static ReconciliationResult pending(
                SubscriptionCheckoutSession checkoutSession,
                PlatformTenant tenant,
                PlatformPlan plan,
                String message
        ) {
            return new ReconciliationResult(
                    "PENDING",
                    false,
                    false,
                    false,
                    true,
                    checkoutSession,
                    tenant,
                    plan,
                    message
            );
        }

        public static ReconciliationResult failed(
                SubscriptionCheckoutSession checkoutSession,
                PlatformTenant tenant,
                PlatformPlan plan,
                String message
        ) {
            return new ReconciliationResult(
                    "FAILED",
                    false,
                    false,
                    true,
                    false,
                    checkoutSession,
                    tenant,
                    plan,
                    message
            );
        }
    }
}
