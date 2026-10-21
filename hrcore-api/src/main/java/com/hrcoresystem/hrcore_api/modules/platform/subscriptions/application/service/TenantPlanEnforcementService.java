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

import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /**
     * Enforces product entitlements independently from role permissions. The
     * OWNER_ADMIN role intentionally keeps its permissions, but cannot use a
     * module that is not part of the active subscription.
     */
    public void assertModuleIncluded(String module) {
        TenantSubscriptionPolicySnapshot policy = currentPolicy();
        assertTenantOperational(policy);
        String normalized = module == null ? "" : module.trim().toLowerCase();
        if (!getIncludedModulesForPlan(policy.planCode()).contains(normalized)) {
            throw new TenantSubscriptionAccessDeniedException(
                    "El módulo '" + normalized + "' no está incluido en el plan activo '"
                            + policy.planCode() + "'. Actualiza tu plan para continuar."
            );
        }
    }

    public void assertCanCreateUser(long currentActiveUsers) {
        TenantSubscriptionPolicySnapshot policy = currentPolicy();
        assertTenantOperational(policy);

        if (currentActiveUsers >= policy.maxUsers()) {
            throw new TenantSubscriptionAccessDeniedException(
                    "El plan contratado '" + policy.planCode() + "' permite un máximo de "
                    + policy.maxUsers() + " usuarios (activos o pendientes). Para registrar más usuarios debe mejorar su plan de suscripción."
            );
        }
    }

    public void assertCanActivateUser(long currentActiveUsers) {
        TenantSubscriptionPolicySnapshot policy = currentPolicy();
        assertTenantOperational(policy);

        if (currentActiveUsers >= policy.maxUsers()) {
            throw new TenantSubscriptionAccessDeniedException(
                    "El plan contratado '" + policy.planCode() + "' permite un máximo de "
                    + policy.maxUsers() + " usuarios activos. Para activar más usuarios debe desactivar otro usuario o mejorar su plan."
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

    public Set<String> getIncludedModulesForPlan(String planCode) {
        if (planCode == null) return Set.of();
        String code = planCode.trim().toUpperCase();
        return switch (code) {
            case "BASIC" -> Set.of(
                    "users", "departments", "positions", "employees", "contracts",
                    "employee_files", "tenantsettings", "dashboard", "notifications"
            );
            case "PRO", "PROFESSIONAL" -> Set.of(
                    "users", "departments", "positions", "employees", "contracts",
                    "employee_files", "shifts", "attendance", "leaves",
                    "tenantsettings", "dashboard", "notifications"
            );
            case "ENTERPRISE", "DEMO", "FREE" -> Set.of(
                    "users", "departments", "positions", "employees", "contracts",
                    "employee_files", "shifts", "attendance", "leaves", "incidents",
                    "hr.reports", "backups", "tenantsettings", "dashboard", "notifications"
            );
            default -> Set.of(
                    "users", "departments", "positions", "employees", "contracts",
                    "employee_files", "tenantsettings", "dashboard", "notifications"
            );
        };
    }

    public boolean isModuleIncludedInCurrentPlan(String module) {
        if (module == null || module.isBlank()) return false;
        try {
            TenantSubscriptionPolicySnapshot policy = currentPolicy();
            return getIncludedModulesForPlan(policy.planCode()).contains(module.trim().toLowerCase());
        } catch (Exception e) {
            return true;
        }
    }

    public void assertCanAssignPermissions(List<String> permissionCodes, Map<String, String> codeToModuleMap) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return;
        }

        TenantSubscriptionPolicySnapshot policy = currentPolicy();
        assertTenantOperational(policy);

        Set<String> allowedModules = getIncludedModulesForPlan(policy.planCode());

        for (String permissionCode : permissionCodes) {
            if (permissionCode == null || permissionCode.isBlank()) continue;
            String normalizedCode = permissionCode.trim();
            String module = codeToModuleMap != null ? codeToModuleMap.get(normalizedCode) : null;
            if (module == null) {
                int firstDot = normalizedCode.indexOf('.');
                module = firstDot > 0 ? normalizedCode.substring(0, firstDot) : normalizedCode;
            }

            if (!allowedModules.contains(module.toLowerCase())) {
                throw new TenantSubscriptionAccessDeniedException(
                        "El módulo '" + module + "' (permiso: " + normalizedCode
                        + ") no está incluido en el plan activo '" + policy.planCode()
                        + "'. Actualiza tu plan de suscripción para asignar permisos sobre este módulo."
                );
            }
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
