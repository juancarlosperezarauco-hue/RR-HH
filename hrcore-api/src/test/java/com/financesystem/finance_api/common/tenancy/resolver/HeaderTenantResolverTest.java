package com.hrcoresystem.hrcore_api.common.tenancy.resolver;

import com.hrcoresystem.hrcore_api.common.tenancy.TenancyProperties;
import com.hrcoresystem.hrcore_api.common.tenancy.schema.TenantSchemaNamingStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeaderTenantResolverTest {

    @Test
    void shouldResolveRegisteredTenant() {
        TenancyProperties properties = new TenancyProperties();
        properties.setHeaderName("X-Tenant-Slug");
        properties.setTenantSchemaPrefix("tenant_");

        TenantSchemaNamingStrategy namingStrategy = new TenantSchemaNamingStrategy(properties);
        HeaderTenantResolver resolver = new HeaderTenantResolver(properties, namingStrategy);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Slug", "FinanCruz");

        var context = resolver.resolve(request);

        assertEquals("financruz", context.tenantSlug());
        assertEquals("tenant_financruz", context.schemaName());
    }

    @Test
    void shouldRejectUnknownTenantSlugBeforeTenantQueries() {
        TenancyProperties properties = new TenancyProperties();
        properties.setHeaderName("X-Tenant-Slug");
        properties.setTenantSchemaPrefix("tenant_");

        TenantSchemaNamingStrategy namingStrategy = new TenantSchemaNamingStrategy(properties);
        HeaderTenantResolver resolver = new HeaderTenantResolver(properties, namingStrategy);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Slug", "FinanCru"); // missing a letter

        var context = resolver.resolve(request);

        assertEquals("financru", context.tenantSlug());
        assertEquals("tenant_financru", context.schemaName());
    }

    @Test
    void shouldResolvePublicPathWithoutTenantHeader() {
        TenancyProperties properties = new TenancyProperties();
        properties.setHeaderName("X-Tenant-Slug");
        properties.setPublicSchema("public");
        properties.setTenantSchemaPrefix("tenant_");
        properties.setPublicPaths(java.util.List.of("/api/public/**"));
        properties.setGlobalPaths(java.util.List.of("/api/platform/**"));

        TenantSchemaNamingStrategy namingStrategy = new TenantSchemaNamingStrategy(properties);
        HeaderTenantResolver resolver = new HeaderTenantResolver(properties, namingStrategy);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/public/checkout-sessions/cs_test/status");

        var context = resolver.resolve(request);

        assertEquals(null, context.tenantSlug());
        assertEquals("public", context.schemaName());
        assertEquals(true, context.publicRequest());
    }

    @Test
    void shouldResolveGlobalPathWithoutTenantHeader() {
        TenancyProperties properties = new TenancyProperties();
        properties.setHeaderName("X-Tenant-Slug");
        properties.setPublicSchema("public");
        properties.setTenantSchemaPrefix("tenant_");
        properties.setPublicPaths(java.util.List.of("/api/public/**"));
        properties.setGlobalPaths(java.util.List.of("/api/platform/**"));

        TenantSchemaNamingStrategy namingStrategy = new TenantSchemaNamingStrategy(properties);
        HeaderTenantResolver resolver = new HeaderTenantResolver(properties, namingStrategy);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/platform/billing/configuration-status");

        var context = resolver.resolve(request);

        assertEquals(null, context.tenantSlug());
        assertEquals("public", context.schemaName());
        assertEquals(true, context.publicRequest());
    }
}
