package com.hrcoresystem.hrcore_api.modules.identity.audit;

import java.util.LinkedHashMap;
import java.util.Map;

public final class IdentityAuditPayloads {

    private IdentityAuditPayloads() {
    }

    public static Map<String, Object> of(Object... keyValues) {
        if (keyValues == null || keyValues.length == 0) {
            return Map.of();
        }

        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Key-value array must contain an even number of elements");
        }

        Map<String, Object> values = new LinkedHashMap<>();
        for (int index = 0; index < keyValues.length; index += 2) {
            Object key = keyValues[index];
            Object value = keyValues[index + 1];

            if (!(key instanceof String keyName) || keyName.isBlank()) {
                continue;
            }

            if (value != null) {
                values.put(keyName, value);
            }
        }

        return values;
    }

    public static Map<String, Object> userState(
            String email,
            String firstName,
            String lastName,
            boolean active,
            String status
    ) {
        return of(
                "email", email,
                "firstName", firstName,
                "lastName", lastName,
                "active", active,
                "status", status
        );
    }

    public static Map<String, Object> roleState(
            String name,
            String description,
            boolean active,
            int permissionCount
    ) {
        return of(
                "name", name,
                "description", description,
                "active", active,
                "permissionCount", permissionCount
        );
    }
}
