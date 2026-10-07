package com.hrcoresystem.hrcore_api.modules.platform.billing.application.usecase;

import com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto.PublicCheckoutActivationStatusResponse;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.service.StripeCheckoutReconciliationService;
import org.springframework.stereotype.Service;

@Service
public class GetPublicCheckoutActivationStatusUseCase {

    private final StripeCheckoutReconciliationService reconciliationService;

    public GetPublicCheckoutActivationStatusUseCase(
            StripeCheckoutReconciliationService reconciliationService
    ) {
        this.reconciliationService = reconciliationService;
    }

    public PublicCheckoutActivationStatusResponse execute(String stripeSessionId) {
        StripeCheckoutReconciliationService.ReconciliationResult result =
                reconciliationService.reconcileByStripeSessionId(stripeSessionId);

        return new PublicCheckoutActivationStatusResponse(
                result.checkoutSession().id(),
                result.checkoutSession().stripeSessionId(),
                result.tenant().id(),
                result.tenant().slug(),
                result.plan().code(),
                result.checkoutSession().billingInterval().name(),
                result.checkoutSession().status(),
                result.activationStatus(),
                result.paid(),
                result.active(),
                result.failed(),
                result.pending(),
                result.message(),
                result.checkoutSession().expiresAt()
        );
    }
}
