package com.hrcoresystem.hrcore_api.modules.platform.audit;

import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscription;
import com.hrcoresystem.hrcore_api.modules.platform.superadmin.domain.model.PlatformSuperadmin;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSetting;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlatformAuditPayloads {

    private PlatformAuditPayloads() {
    }

    public static Map<String, Object> details(Object... keyValues) {
        if (keyValues == null || keyValues.length == 0) {
            return Map.of();
        }

        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Audit details must contain key-value pairs");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            payload.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }

        return payload;
    }

    public static Map<String, Object> tenantState(PlatformTenant tenant) {
        return details(
                "id", tenant.id(),
                "name", tenant.name(),
                "slug", tenant.slug(),
                "schemaName", tenant.schemaName(),
                "status", tenant.status(),
                "planId", tenant.planId(),
                "active", tenant.active(),
                "createdAt", tenant.createdAt(),
                "updatedAt", tenant.updatedAt()
        );
    }

    public static Map<String, Object> planState(PlatformPlan plan) {
        return details(
                "id", plan.id(),
                "code", plan.code(),
                "name", plan.name(),
                "description", plan.description(),
                "maxUsers", plan.maxUsers(),
                "maxRoles", plan.maxRoles(),
                "planType", plan.planType(),
                "trialDays", plan.trialDays(),
                "active", plan.active(),
                "createdAt", plan.createdAt(),
                "updatedAt", plan.updatedAt()
        );
    }

    public static Map<String, Object> subscriptionState(PlatformSubscription subscription) {
        return details(
                "id", subscription.id(),
                "tenantId", subscription.tenantId(),
                "planId", subscription.planId(),
                "status", subscription.status(),
                "trial", subscription.trial(),
                "currentSubscription", subscription.currentSubscription(),
                "startedAt", subscription.startedAt(),
                "expiresAt", subscription.expiresAt(),
                "createdAt", subscription.createdAt(),
                "updatedAt", subscription.updatedAt()
        );
    }

    public static Map<String, Object> settingsState(List<TenantSetting> settings) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (settings == null) {
            return payload;
        }

        for (TenantSetting setting : settings) {
            payload.put(setting.settingKey(), setting.settingValue());
        }

        return payload;
    }

    public static Map<String, Object> superadminState(PlatformSuperadmin superadmin) {
        return details(
                "id", superadmin.id(),
                "email", superadmin.email(),
                "firstName", superadmin.firstName(),
                "lastName", superadmin.lastName(),
                "active", superadmin.active(),
                "createdAt", superadmin.createdAt(),
                "updatedAt", superadmin.updatedAt()
        );
    }
}
