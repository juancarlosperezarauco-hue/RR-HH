package com.hrcoresystem.hrcore_api.modules.platform.billing.application.job;

import com.hrcoresystem.hrcore_api.modules.platform.billing.application.service.StripeCheckoutReconciliationService;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.model.SubscriptionCheckoutSession;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.repository.SubscriptionCheckoutSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StripeCheckoutReconciliationJob {

    private static final Logger logger = LoggerFactory.getLogger(StripeCheckoutReconciliationJob.class);

    private final SubscriptionCheckoutSessionRepository checkoutSessionRepository;
    private final StripeCheckoutReconciliationService reconciliationService;

    public StripeCheckoutReconciliationJob(
            SubscriptionCheckoutSessionRepository checkoutSessionRepository,
            StripeCheckoutReconciliationService reconciliationService
    ) {
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.reconciliationService = reconciliationService;
    }

    @Scheduled(fixedDelayString = "${hrcore.billing.checkout-reconciliation-ms:60000}")
    public void reconcilePendingCheckoutSessions() {
        List<SubscriptionCheckoutSession> candidates = checkoutSessionRepository.findPendingActivationCandidates();

        if (candidates.isEmpty()) {
            return;
        }

        logger.info("[STRIPE_CHECKOUT_RECONCILE_JOB] pendingCandidates={}", candidates.size());

        for (SubscriptionCheckoutSession session : candidates) {
            try {
                reconciliationService.reconcileByStripeSessionId(session.stripeSessionId());
            } catch (Exception exception) {
                logger.warn(
                        "[STRIPE_CHECKOUT_RECONCILE_JOB_FAILED] checkoutSessionId={} stripeSessionId={} error={}",
                        session.id(),
                        session.stripeSessionId(),
                        exception.getMessage()
                );
            }
        }
    }
}
