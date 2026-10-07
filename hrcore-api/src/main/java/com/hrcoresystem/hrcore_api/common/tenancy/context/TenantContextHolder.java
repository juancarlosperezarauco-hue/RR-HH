package com.hrcoresystem.hrcore_api.common.tenancy.context;

import com.hrcoresystem.hrcore_api.common.tenancy.exception.TenantResolutionException;

public final class TenantContextHolder {

    private static final ThreadLocal<TenantContext> CONTEXT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void set(TenantContext tenantContext) {
        CONTEXT.set(tenantContext);
    }

    public static TenantContext get() {
        return CONTEXT.get();
    }

    public static TenantContext getRequired() {
        TenantContext context = CONTEXT.get();
        if (context == null) {
            throw new TenantResolutionException("Tenant context is not available for the current request");
        }
        return context;
    }

    public static String getCurrentSchemaOrDefault(String defaultSchema) {
        TenantContext context = CONTEXT.get();
        if (context == null || context.schemaName() == null || context.schemaName().isBlank()) {
            return defaultSchema;
        }
        return context.schemaName();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}