package com.hrcoresystem.hrcore_api.modules.platform.billing.application.service;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.exception.BillingException;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.model.SubscriptionCheckoutSession;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.model.SubscriptionPayment;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.repository.SubscriptionCheckoutSessionRepository;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.repository.SubscriptionPaymentRepository;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.BillingInterval;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscription;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscriptionStatus;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.repository.PlatformSubscriptionRepository;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.InvoicePayment;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class StripeSubscriptionSyncService {

    private final SubscriptionCheckoutSessionRepository checkoutSessionRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final PlatformSubscriptionRepository subscriptionRepository;
    private final PlatformTenantRepository tenantRepository;
    private final PlatformPlanRepository planRepository;
    private final AuditTrailService auditTrailService;

    public StripeSubscriptionSyncService(
            SubscriptionCheckoutSessionRepository checkoutSessionRepository,
            SubscriptionPaymentRepository paymentRepository,
            PlatformSubscriptionRepository subscriptionRepository,
            PlatformTenantRepository tenantRepository,
            PlatformPlanRepository planRepository,
            AuditTrailService auditTrailService
    ) {
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public void handleCheckoutSessionCompleted(Event event) {
        Session session = deserialize(event, Session.class);

        SubscriptionCheckoutSession checkoutSession = checkoutSessionRepository.findByStripeSessionId(session.getId())
                .orElseThrow(() -> new BillingException("Checkout session not found: " + session.getId()));

        SubscriptionCheckoutSession completed = new SubscriptionCheckoutSession(
                checkoutSession.id(),
                checkoutSession.tenantId(),
                checkoutSession.planId(),
                checkoutSession.requestedByUserId(),
                checkoutSession.requestedByEmail(),
                checkoutSession.billingInterval(),
                "COMPLETED",
                checkoutSession.stripeCustomerId(),
                checkoutSession.stripeSessionId(),
                resolveSubscriptionId(session),
                checkoutSession.stripePriceId(),
                checkoutSession.checkoutUrl(),
                checkoutSession.successUrl(),
                checkoutSession.cancelUrl(),
                checkoutSession.amount(),
                checkoutSession.currency(),
                Instant.now(),
                checkoutSession.expiresAt(),
                checkoutSession.createdAt(),
                checkoutSession.updatedAt()
        );

        checkoutSessionRepository.save(completed);

        String stripeSubscriptionId = resolveSubscriptionId(session);
        if (StringUtils.hasText(stripeSubscriptionId)) {
            syncStripeSubscription(stripeSubscriptionId, checkoutSession.tenantId());
        }

        auditTrailService.recordPlatformEvent(
                AuditEventTypes.SUBSCRIPTION_CHECKOUT_COMPLETED,
                "SUBSCRIPTION_CHECKOUT",
                completed.id().toString(),
                "Checkout completed for tenant " + completed.tenantId()
        );
    }

    @Transactional
    public void handleSubscriptionChanged(Event event) {
        Subscription subscription = deserialize(event, Subscription.class);
        syncSubscriptionObject(subscription, null);
    }

    @Transactional
    public void handleInvoicePaymentSucceeded(Event event) {
        Invoice invoice = deserialize(event, Invoice.class);

        String stripeSubscriptionId = resolveSubscriptionId(invoice);
        if (StringUtils.hasText(stripeSubscriptionId)) {
            syncStripeSubscription(stripeSubscriptionId, null);
        }

        savePayment(invoice, "PAID", null);

        auditTrailService.recordPlatformEvent(
                AuditEventTypes.SUBSCRIPTION_PAYMENT_SUCCEEDED,
                "SUBSCRIPTION_PAYMENT",
                invoice.getId(),
                "Stripe invoice paid successfully"
        );
    }

    @Transactional
    public void handleInvoicePaymentFailed(Event event) {
        Invoice invoice = deserialize(event, Invoice.class);

        String stripeSubscriptionId = resolveSubscriptionId(invoice);
        if (StringUtils.hasText(stripeSubscriptionId)) {
            syncStripeSubscription(stripeSubscriptionId, null);
        }

        savePayment(invoice, "FAILED", "Stripe invoice payment failed");

        auditTrailService.recordPlatformEvent(
                AuditEventTypes.SUBSCRIPTION_PAYMENT_FAILED,
                "SUBSCRIPTION_PAYMENT",
                invoice.getId(),
                "Stripe invoice payment failed"
        );
    }

    private void syncStripeSubscription(String stripeSubscriptionId, UUID tenantIdHint) {
        try {
            Subscription subscription = Subscription.retrieve(stripeSubscriptionId);
            syncSubscriptionObject(subscription, tenantIdHint);
        } catch (Exception exception) {
            throw new BillingException("Could not retrieve Stripe subscription: " + stripeSubscriptionId, exception);
        }
    }

    private void syncSubscriptionObject(Subscription stripeSubscription, UUID tenantIdHint) {
        UUID tenantId = resolveTenantId(stripeSubscription, tenantIdHint);
        PlatformPlan plan = resolvePlan(stripeSubscription);
        BillingInterval billingInterval = resolveBillingInterval(stripeSubscription);

        PlatformTenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BillingException("Tenant not found for Stripe subscription"));

        PlatformSubscriptionStatus status = mapStatus(stripeSubscription.getStatus());
        PlatformSubscription previous = subscriptionRepository.findCurrentByTenantId(tenant.id()).orElse(null);

        subscriptionRepository.clearCurrentSubscriptions(tenant.id());

        PlatformSubscription current = subscriptionRepository.save(
                new PlatformSubscription(
                        previous != null && stripeSubscription.getId().equals(previous.stripeSubscriptionId())
                                ? previous.id()
                                : null,
                        tenant.id(),
                        plan.id(),
                        status,
                        false,
                        true,
                        stripeSubscription.getId(),
                        null,
                        billingInterval,
                        toInstant(stripeSubscription.getStartDate()),
                        null,
                        resolveCurrentPeriodStart(stripeSubscription),
                        resolveCurrentPeriodEnd(stripeSubscription),
                        Boolean.TRUE.equals(stripeSubscription.getCancelAtPeriodEnd()),
                        toInstant(stripeSubscription.getCanceledAt()),
                        previous == null ? null : previous.createdAt(),
                        previous == null ? null : previous.updatedAt()
                )
        );

        tenantRepository.save(
                new PlatformTenant(
                        tenant.id(),
                        tenant.name(),
                        tenant.slug(),
                        tenant.schemaName(),
                        tenant.status(),
                        plan.id(),
                        tenant.stripeCustomerId(),
                        tenant.active(),
                        tenant.createdAt(),
                        tenant.updatedAt()
                )
        );

        auditTrailService.recordPlatformEvent(
                resolveAuditEvent(previous, current),
                "TENANT_SUBSCRIPTION",
                current.id().toString(),
                "Stripe subscription synchronized"
        );
    }

    private void savePayment(Invoice invoice, String status, String failureReason) {
        if (paymentRepository.findByStripeInvoiceId(invoice.getId()).isPresent()) {
            return;
        }

        String stripeSubscriptionId = resolveSubscriptionId(invoice);

        InvoiceContext context = resolveInvoiceContext(invoice, stripeSubscriptionId);

        paymentRepository.save(
                new SubscriptionPayment(
                        null,
                        context.tenant().id(),
                        context.platformSubscription() == null ? null : context.platformSubscription().id(),
                        context.plan().id(),
                        invoice.getCustomer(),
                        stripeSubscriptionId,
                        invoice.getId(),
                        resolvePaymentIntent(invoice),
                        resolveCharge(invoice),
                        invoice.getNumber(),
                        invoice.getHostedInvoiceUrl(),
                        invoice.getInvoicePdf(),
                        BigDecimal.valueOf(invoice.getAmountPaid() == null ? 0 : invoice.getAmountPaid())
                                .movePointLeft(2),
                        invoice.getCurrency() == null ? "USD" : invoice.getCurrency().toUpperCase(),
                        status,
                        invoice.getBillingReason(),
                        "PAID".equals(status) ? Instant.now() : null,
                        "FAILED".equals(status) ? Instant.now() : null,
                        failureReason,
                        null,
                        null
                )
        );
    }

    private UUID resolveTenantId(Subscription subscription, UUID tenantIdHint) {
        if (tenantIdHint != null) {
            return tenantIdHint;
        }

        String tenantIdValue = subscription.getMetadata().get("tenant_id");
        if (!StringUtils.hasText(tenantIdValue)) {
            String stripeCustomerId = subscription.getCustomer();
            if (StringUtils.hasText(stripeCustomerId)) {
                PlatformTenant tenantByCustomer = tenantRepository.findByStripeCustomerId(stripeCustomerId)
                        .orElse(null);
                if (tenantByCustomer != null) {
                    return tenantByCustomer.id();
                }
            }

            if (StringUtils.hasText(subscription.getId())) {
                return checkoutSessionRepository.findByTenantIdFromStripeSubscription(subscription.getId())
                        .orElseThrow(() -> new BillingException("Stripe subscription is missing tenant metadata"));
            }

            throw new BillingException("Stripe subscription is missing tenant metadata");
        }

        return UUID.fromString(tenantIdValue.trim());
    }

    private PlatformPlan resolvePlan(Subscription subscription) {
        if (subscription.getMetadata() == null) {
            throw new BillingException("Stripe subscription is missing plan metadata");
        }

        String planIdValue = subscription.getMetadata().get("plan_id");
        if (StringUtils.hasText(planIdValue)) {
            UUID planId = UUID.fromString(planIdValue.trim());
            return planRepository.findById(planId)
                    .orElseThrow(() -> new BillingException("Plan not found for Stripe subscription metadata"));
        }

        String planCode = subscription.getMetadata().get("plan_code");
        if (StringUtils.hasText(planCode)) {
            return planRepository.findByCode(planCode.trim().toUpperCase())
                    .orElseThrow(() -> new BillingException("Plan not found for Stripe subscription: " + planCode));
        }

        throw new BillingException("Stripe subscription is missing plan metadata");
    }

    private BillingInterval resolveBillingInterval(Subscription subscription) {
        if (subscription.getMetadata() == null) {
            throw new BillingException("Stripe subscription is missing billing_interval metadata");
        }

        String billingIntervalValue = subscription.getMetadata().get("billing_interval");
        if (!StringUtils.hasText(billingIntervalValue)) {
            throw new BillingException("Stripe subscription is missing billing_interval metadata");
        }

        return BillingInterval.valueOf(billingIntervalValue.trim().toUpperCase());
    }

    private Instant resolveCurrentPeriodStart(Subscription subscription) {
        if (subscription.getLatestInvoiceObject() != null) {
            return toInstant(subscription.getLatestInvoiceObject().getPeriodStart());
        }

        return toInstant(subscription.getBillingCycleAnchor());
    }

    private Instant resolveCurrentPeriodEnd(Subscription subscription) {
        if (subscription.getLatestInvoiceObject() != null) {
            return toInstant(subscription.getLatestInvoiceObject().getPeriodEnd());
        }

        return null;
    }

    private String resolveSubscriptionId(Session session) {
        if (session.getSubscription() != null) {
            return session.getSubscription();
        }

        if (session.getSubscriptionObject() != null) {
            return session.getSubscriptionObject().getId();
        }

        return null;
    }

    private String resolveSubscriptionId(Invoice invoice) {
        if (invoice.getParent() != null
                && invoice.getParent().getSubscriptionDetails() != null
                && StringUtils.hasText(invoice.getParent().getSubscriptionDetails().getSubscription())) {
            return invoice.getParent().getSubscriptionDetails().getSubscription();
        }

        return null;
    }

    private String resolvePaymentIntent(Invoice invoice) {
        if (invoice.getPayments() == null || invoice.getPayments().getData().isEmpty()) {
            return null;
        }

        InvoicePayment invoicePayment = invoice.getPayments().getData().get(0);
        if (invoicePayment.getPayment() == null) {
            return null;
        }

        return invoicePayment.getPayment().getPaymentIntent();
    }

    private String resolveCharge(Invoice invoice) {
        if (invoice.getPayments() == null || invoice.getPayments().getData().isEmpty()) {
            return null;
        }

        InvoicePayment invoicePayment = invoice.getPayments().getData().get(0);
        if (invoicePayment.getPayment() == null) {
            return null;
        }

        return invoicePayment.getPayment().getCharge();
    }

    private InvoiceContext resolveInvoiceContext(Invoice invoice, String stripeSubscriptionId) {
        PlatformSubscription platformSubscription = StringUtils.hasText(stripeSubscriptionId)
                ? subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId).orElse(null)
                : null;

        PlatformTenant tenant = null;
        PlatformPlan plan = null;

        if (platformSubscription != null) {
            tenant = tenantRepository.findById(platformSubscription.tenantId()).orElse(null);
            plan = planRepository.findById(platformSubscription.planId()).orElse(null);
        }

        if (tenant == null && StringUtils.hasText(invoice.getCustomer())) {
            tenant = tenantRepository.findByStripeCustomerId(invoice.getCustomer()).orElse(null);
        }

        if ((tenant == null || plan == null) && StringUtils.hasText(stripeSubscriptionId)) {
            SubscriptionCheckoutSession checkoutSession = checkoutSessionRepository
                    .findByStripeSubscriptionId(stripeSubscriptionId)
                    .orElse(null);
            if (checkoutSession != null) {
                if (tenant == null) {
                    tenant = tenantRepository.findById(checkoutSession.tenantId()).orElse(null);
                }
                if (plan == null) {
                    plan = planRepository.findById(checkoutSession.planId()).orElse(null);
                }
            }
        }

        if (plan == null && StringUtils.hasText(stripeSubscriptionId)) {
            try {
                Subscription subscription = Subscription.retrieve(stripeSubscriptionId);
                plan = resolvePlan(subscription);
                if (tenant == null) {
                    tenant = tenantRepository.findById(resolveTenantId(subscription, null)).orElse(null);
                }
            } catch (Exception ignored) {
                // fall through to explicit error below
            }
        }

        if (tenant == null) {
            throw new BillingException("Could not resolve tenant for Stripe invoice: " + invoice.getId());
        }

        if (plan == null) {
            throw new BillingException("Could not resolve plan for Stripe invoice: " + invoice.getId());
        }

        return new InvoiceContext(platformSubscription, tenant, plan);
    }

    private PlatformSubscriptionStatus mapStatus(String stripeStatus) {
        if (stripeStatus == null) {
            return PlatformSubscriptionStatus.INCOMPLETE;
        }

        return switch (stripeStatus) {
            case "active", "trialing" -> PlatformSubscriptionStatus.ACTIVE;
            case "past_due" -> PlatformSubscriptionStatus.PAST_DUE;
            case "unpaid" -> PlatformSubscriptionStatus.SUSPENDED;
            case "canceled" -> PlatformSubscriptionStatus.CANCELLED;
            case "incomplete", "incomplete_expired" -> PlatformSubscriptionStatus.INCOMPLETE;
            default -> PlatformSubscriptionStatus.INCOMPLETE;
        };
    }

    private String resolveAuditEvent(PlatformSubscription previous, PlatformSubscription current) {
        if (current.status() == PlatformSubscriptionStatus.CANCELLED) {
            return AuditEventTypes.SUBSCRIPTION_CANCELLED;
        }

        if (previous == null) {
            return AuditEventTypes.SUBSCRIPTION_UPGRADED;
        }

        if (!previous.planId().equals(current.planId())) {
            return AuditEventTypes.SUBSCRIPTION_UPGRADED;
        }

        return AuditEventTypes.SUBSCRIPTION_RENEWED;
    }

    private Instant toInstant(Long epochSeconds) {
        return epochSeconds == null ? null : Instant.ofEpochSecond(epochSeconds);
    }

    private <T> T deserialize(Event event, Class<T> type) {
        Object object = event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new BillingException("Could not deserialize Stripe event: " + event.getType()));

        return type.cast(object);
    }

    private record InvoiceContext(
            PlatformSubscription platformSubscription,
            PlatformTenant tenant,
            PlatformPlan plan
    ) {
    }
}
