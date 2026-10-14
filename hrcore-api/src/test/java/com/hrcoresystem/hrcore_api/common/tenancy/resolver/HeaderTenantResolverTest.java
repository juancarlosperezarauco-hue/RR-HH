package com.hrcoresystem.hrcore_api.common.tenancy.resolver;

import com.hrcoresystem.hrcore_api.common.tenancy.TenancyProperties;
import com.hrcoresystem.hrcore_api.common.tenancy.exception.TenantResolutionException;
import com.hrcoresystem.hrcore_api.common.tenancy.schema.TenantSchemaNamingStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class HeaderTenantResolverTest {
    private HeaderTenantResolver resolver(TenancyProperties properties) {
        return new HeaderTenantResolver(properties, new TenantSchemaNamingStrategy(properties), mock(DataSource.class));
    }

    @Test
    void shouldRejectMissingTenantHeader() {
        TenancyProperties properties = new TenancyProperties();
        properties.setHeaderName("X-Tenant-Slug");
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThrows(TenantResolutionException.class, () -> resolver(properties).resolve(request));
    }

    @Test
    void shouldRejectNonCanonicalTenantSlugBeforeDatabaseLookup() {
        TenancyProperties properties = new TenancyProperties();
        properties.setHeaderName("X-Tenant-Slug");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Slug", "TalentoHumano");
        assertThrows(TenantResolutionException.class, () -> resolver(properties).resolve(request));
    }

    @Test
    void shouldResolvePublicPathWithoutTenantHeader() {
        TenancyProperties properties = new TenancyProperties();
        properties.setHeaderName("X-Tenant-Slug");
        properties.setPublicSchema("public");
        properties.setPublicPaths(java.util.List.of("/api/public/**"));
        HeaderTenantResolver resolver = resolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/public/status");
        var context = resolver.resolve(request);
        assertNull(context.tenantSlug());
        assertEquals("public", context.schemaName());
        assertTrue(context.publicRequest());
    }

    @Test
    void shouldResolveGlobalPathWithoutTenantHeader() {
        TenancyProperties properties = new TenancyProperties();
        properties.setHeaderName("X-Tenant-Slug");
        properties.setPublicSchema("public");
        properties.setGlobalPaths(java.util.List.of("/api/platform/**"));
        HeaderTenantResolver resolver = resolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/platform/status");
        var context = resolver.resolve(request);
        assertNull(context.tenantSlug());
        assertEquals("public", context.schemaName());
        assertTrue(context.publicRequest());
    }
}
