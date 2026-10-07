package com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service;

import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.exception.PlatformSubscriptionNotFoundException;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.exception.TenantSubscriptionAccessDeniedException;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscription;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscriptionStatus;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.TenantSubscriptionPolicySnapshot;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.repository.PlatformSubscriptionRepository;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.exception.PlatformTenantNotFoundException;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import org.springframework.stereotype.Service;

@Service
public class TenantPlanEnforcementService {

    private final PlatformTenantRepository platformTenantRepository;
    private final PlatformSubscriptionRepository platformSubscriptionRepository;
    private final PlatformPlanRepository platformPlanRepository;
    private final PlatformSubscriptionLifecycleService platformSubscriptionLifecycleService;

    public TenantPlanEnforcementService(
            PlatformTenantRepository platformTenantRepository,
            PlatformSubscriptionRepository platformSubscriptionRepository,
            PlatformPlanRepository platformPlanRepository,
            PlatformSubscriptionLifecycleService platformSubscriptionLifecycleService
    ) {
        this.platformTenantRepository = platformTenantRepository;
        this.platformSubscriptionRepository = platformSubscriptionRepository;
        this.platformPlanRepository = platformPlanRepository;
        this.platformSubscriptionLifecycleService = platformSubscriptionLifecycleService;
    }

    public TenantSubscriptionPolicySnapshot currentPolicy() {
        platformSubscriptionLifecycleService.refreshExpiredSubscriptions();

        String tenantSlug = TenantContextHolder.getRequired().tenantSlug();

        PlatformTenant tenant = platformTenantRepository.findBySlug(tenantSlug)
                .orElseThrow(() -> new PlatformTenantNotFoundException(
                "Tenant not found for slug: " + tenantSlug
        ));

        PlatformSubscription subscription = platformSubscriptionRepository.findCurrentByTenantId(tenant.id())
                .orElseThrow(() -> new PlatformSubscriptionNotFoundException(
                "Current subscription not found for tenant: " + tenantSlug
        ));

        PlatformPlan plan = platformPlanRepository.findById(subscription.planId())
                .orElseThrow(() -> new PlatformSubscriptionNotFoundException(
                "Plan not found for current subscription"
        ));

        return new TenantSubscriptionPolicySnapshot(
                tenant.id(),
                tenant.slug(),
                tenant.active(),
                tenant.status().name(),
                subscription.id(),
                subscription.status(),
                subscription.trial(),
                subscription.expiresAt(),
                plan.id(),
                plan.code(),
                plan.planType(),
                plan.maxUsers(),
                plan.maxRoles()
        );
    }

    public TenantSubscriptionPolicySnapshot resolveCurrentTenantPolicy() {
        return currentPolicy();
    }

    public void assertCanOperate() {
        assertTenantOperational(currentPolicy());
    }

    public void assertCanCreateUser(long currentActiveUsers) {
        TenantSubscriptionPolicySnapshot policy = currentPolicy();
        assertTenantOperational(policy);

        if (currentActiveUsers >= policy.maxUsers()) {
            throw new TenantSubscriptionAccessDeniedException(
                    "Your current plan '" + policy.planCode() + "' allows a maximum of "
                    + policy.maxUsers() + " users (active or pending). Upgrade is required to create more users."
            );
        }
    }

    public void assertCanActivateUser(long currentActiveUsers) {
        TenantSubscriptionPolicySnapshot policy = currentPolicy();
        assertTenantOperational(policy);

        if (currentActiveUsers >= policy.maxUsers()) {
            throw new TenantSubscriptionAccessDeniedException(
                    "Your current plan '" + policy.planCode() + "' allows a maximum of "
                    + policy.maxUsers() + " users (active or pending). Upgrade is required to activate more users."
            );
        }
    }

    public void assertCanCreateRole(long currentActiveCustomRoles) {
        TenantSubscriptionPolicySnapshot policy = currentPolicy();
        assertTenantOperational(policy);

        if (currentActiveCustomRoles >= policy.maxRoles()) {
            throw new TenantSubscriptionAccessDeniedException(
                    "Your current plan '" + policy.planCode() + "' allows a maximum of "
                    + policy.maxRoles() + " active custom roles. Upgrade is required to create more roles."
            );
        }
    }

    public void assertCanActivateRole(long currentActiveCustomRoles) {
        TenantSubscriptionPolicySnapshot policy = currentPolicy();
        assertTenantOperational(policy);

        if (currentActiveCustomRoles >= policy.maxRoles()) {
            throw new TenantSubscriptionAccessDeniedException(
                    "Your current plan '" + policy.planCode() + "' allows a maximum of "
                    + policy.maxRoles() + " active custom roles. Upgrade is required to activate more roles."
            );
        }
    }

    private void assertTenantOperational(TenantSubscriptionPolicySnapshot policy) {
        if (!policy.tenantActive()) {
            throw new TenantSubscriptionAccessDeniedException(
                    "The tenant is inactive and cannot perform this operation"
            );
        }

        if (policy.subscriptionStatus() != PlatformSubscriptionStatus.ACTIVE
                && policy.subscriptionStatus() != PlatformSubscriptionStatus.TRIAL) {
            throw new TenantSubscriptionAccessDeniedException(
                    "The current subscription status is '" + policy.subscriptionStatus().name()
                    + "'. Upgrade or reactivate the subscription to continue."
            );
        }
    }
}
