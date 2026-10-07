package com.hrcoresystem.hrcore_api.modules.tenant.audit;

import java.util.LinkedHashMap;
import java.util.Map;

/** Generic SaaS audit payload helpers. */
public final class TenantAuditPayloads {
    private TenantAuditPayloads() {
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
}
