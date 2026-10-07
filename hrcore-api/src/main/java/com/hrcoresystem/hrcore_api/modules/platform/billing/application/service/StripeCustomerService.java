package com.hrcoresystem.hrcore_api.modules.platform.billing.application.service;

import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.exception.BillingException;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class StripeCustomerService {

    private final PlatformTenantRepository platformTenantRepository;

    public StripeCustomerService(PlatformTenantRepository platformTenantRepository) {
        this.platformTenantRepository = platformTenantRepository;
    }

    @Transactional
    public PlatformTenant ensureStripeCustomer(PlatformTenant tenant, String ownerEmail) {
        if (StringUtils.hasText(tenant.stripeCustomerId())) {
            return tenant;
        }

        try {
            CustomerCreateParams.Builder params = CustomerCreateParams.builder()
                    .setName(tenant.name())
                    .putMetadata("tenant_id", tenant.id().toString())
                    .putMetadata("tenant_slug", tenant.slug());

            if (StringUtils.hasText(ownerEmail)) {
                params.setEmail(ownerEmail.trim().toLowerCase());
            }

            Customer customer = Customer.create(params.build());

            PlatformTenant updatedTenant = new PlatformTenant(
                    tenant.id(),
                    tenant.name(),
                    tenant.slug(),
                    tenant.schemaName(),
                    tenant.status(),
                    tenant.planId(),
                    customer.getId(),
                    tenant.active(),
                    tenant.createdAt(),
                    tenant.updatedAt()
            );

            return platformTenantRepository.save(updatedTenant);
        } catch (StripeException exception) {
            throw new BillingException("Could not create Stripe customer", exception);
        }
    }
}
