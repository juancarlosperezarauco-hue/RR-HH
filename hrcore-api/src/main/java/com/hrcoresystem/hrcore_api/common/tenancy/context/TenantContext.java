package com.hrcoresystem.hrcore_api.common.tenancy.context;

public record TenantContext(
        String tenantSlug,
        String schemaName,
        boolean publicRequest
) {
    /**
     * Backward-compatible alias for older compiled code that still expects a publicSchema accessor.
     * The current model uses schemaName for both public and tenant contexts.
     */
    public String publicSchema() {
        return schemaName;
    }
}
