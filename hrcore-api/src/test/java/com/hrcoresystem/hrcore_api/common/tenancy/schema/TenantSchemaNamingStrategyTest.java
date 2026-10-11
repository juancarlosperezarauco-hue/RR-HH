package com.hrcoresystem.hrcore_api.common.tenancy.schema;

import com.hrcoresystem.hrcore_api.common.tenancy.TenancyProperties;
import com.hrcoresystem.hrcore_api.common.tenancy.exception.TenantResolutionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantSchemaNamingStrategyTest {

    @Test
    void shouldNormalizeSlugAndBuildSchemaName() {
        TenancyProperties properties = new TenancyProperties();
        properties.setTenantSchemaPrefix("tenant_");

        TenantSchemaNamingStrategy strategy = new TenantSchemaNamingStrategy(properties);

        String normalizedSlug = strategy.normalizeSlug("Finan Cruz");
        String schemaName = strategy.toSchemaName("Finan Cruz");

        assertEquals("finan-cruz", normalizedSlug);
        assertEquals("tenant_finan_cruz", schemaName);
    }

    @Test
    void shouldThrowExceptionWhenSlugIsInvalid() {
        TenancyProperties properties = new TenancyProperties();
        properties.setTenantSchemaPrefix("tenant_");

        TenantSchemaNamingStrategy strategy = new TenantSchemaNamingStrategy(properties);

        assertThrows(TenantResolutionException.class, () -> strategy.normalizeSlug("%%%"));
    }
}