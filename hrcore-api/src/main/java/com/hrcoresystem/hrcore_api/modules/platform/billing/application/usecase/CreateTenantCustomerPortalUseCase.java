package com.hrcoresystem.hrcore_api.modules.platform.billing.application.usecase;

import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto.CustomerPortalResponse;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.service.StripeCustomerPortalService;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.exception.PlatformTenantNotFoundException;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import com.stripe.model.billingportal.Session;
import org.springframework.stereotype.Service;

@Service
public class CreateTenantCustomerPortalUseCase {

    private final PlatformTenantRepository platformTenantRepository;
    private final StripeCustomerPortalService stripeCustomerPortalService;

    public CreateTenantCustomerPortalUseCase(
            PlatformTenantRepository platformTenantRepository,
            StripeCustomerPortalService stripeCustomerPortalService
    ) {
        this.platformTenantRepository = platformTenantRepository;
        this.stripeCustomerPortalService = stripeCustomerPortalService;
    }

    public CustomerPortalResponse execute() {
        String tenantSlug = TenantContextHolder.getRequired().tenantSlug();

        PlatformTenant tenant = platformTenantRepository.findBySlug(tenantSlug)
                .orElseThrow(() -> new PlatformTenantNotFoundException("Tenant not found with slug: " + tenantSlug));

        Session session = stripeCustomerPortalService.createPortalSession(tenant);

        return new CustomerPortalResponse(session.getUrl());
    }
}
