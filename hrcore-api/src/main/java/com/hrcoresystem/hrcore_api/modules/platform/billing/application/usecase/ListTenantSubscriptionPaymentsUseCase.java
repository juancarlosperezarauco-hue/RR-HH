package com.hrcoresystem.hrcore_api.modules.platform.billing.application.usecase;

import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto.SubscriptionPaymentResponse;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.model.SubscriptionPayment;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.repository.SubscriptionPaymentRepository;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.exception.PlatformTenantNotFoundException;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListTenantSubscriptionPaymentsUseCase {

    private final PlatformTenantRepository platformTenantRepository;
    private final SubscriptionPaymentRepository subscriptionPaymentRepository;
    private final PlatformPlanRepository platformPlanRepository;

    public ListTenantSubscriptionPaymentsUseCase(
            PlatformTenantRepository platformTenantRepository,
            SubscriptionPaymentRepository subscriptionPaymentRepository,
            PlatformPlanRepository platformPlanRepository
    ) {
        this.platformTenantRepository = platformTenantRepository;
        this.subscriptionPaymentRepository = subscriptionPaymentRepository;
        this.platformPlanRepository = platformPlanRepository;
    }

    public List<SubscriptionPaymentResponse> execute() {
        String tenantSlug = TenantContextHolder.getRequired().tenantSlug();
        PlatformTenant tenant = platformTenantRepository.findBySlug(tenantSlug)
                .orElseThrow(() -> new PlatformTenantNotFoundException("Tenant not found with slug: " + tenantSlug));

        return subscriptionPaymentRepository.findByTenantId(tenant.id())
                .stream()
                .map(payment -> toResponse(payment, tenant.slug()))
                .toList();
    }

    private SubscriptionPaymentResponse toResponse(SubscriptionPayment payment, String tenantSlug) {
        String planCode = payment.planId() == null
                ? null
                : platformPlanRepository.findById(payment.planId()).map(plan -> plan.code()).orElse(null);

        return new SubscriptionPaymentResponse(
                payment.id(),
                tenantSlug,
                planCode,
                payment.stripeInvoiceId(),
                payment.stripeSubscriptionId(),
                payment.invoiceNumber(),
                payment.amount(),
                payment.currency(),
                payment.status(),
                payment.billingReason(),
                payment.paidAt(),
                payment.failedAt(),
                payment.failureReason(),
                payment.createdAt(),
                payment.updatedAt()
        );
    }
}
