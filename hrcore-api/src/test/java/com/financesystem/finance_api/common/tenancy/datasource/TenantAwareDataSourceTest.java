package com.hrcoresystem.hrcore_api.common.tenancy.datasource;

import com.hrcoresystem.hrcore_api.common.tenancy.TenancyProperties;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.common.tenancy.exception.TenantResolutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class TenantAwareDataSourceTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        TenantContextHolder.clear();
    }

    @Test
    void shouldFailBeforeOpeningConnectionWhenTenantSlugIsInvalid() {
        TenancyProperties properties = new TenancyProperties();
        properties.setHeaderName("X-Tenant-Slug");
        properties.setPublicSchema("public");
        properties.setTenantSchemaPrefix("tenant_");

        DataSource delegate = mock(DataSource.class);
        TenantAwareDataSource dataSource = new TenantAwareDataSource(delegate, properties);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        TenantContextHolder.set(new TenantContext(null, "public", true));

        TenantResolutionException exception = assertThrows(
                TenantResolutionException.class,
                dataSource::getConnection
        );

        assertEquals(
                "Tenant header 'X-Tenant-Slug' is missing or invalid for request path '/api/auth/login'. Verify the tenant slug matches an existing tenant before retrying.",
                exception.getMessage()
        );
        verifyNoInteractions(delegate);
    }

    @Test
    void shouldApplyTenantSchemaForTenantRequests() throws Exception {
        TenancyProperties properties = new TenancyProperties();
        properties.setHeaderName("X-Tenant-Slug");
        properties.setPublicSchema("public");
        properties.setTenantSchemaPrefix("tenant_");

        DataSource delegate = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);

        when(delegate.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        TenantAwareDataSource dataSource = new TenantAwareDataSource(delegate, properties);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        TenantContextHolder.set(new TenantContext("financruz", "tenant_financruz", false));

        Connection resolvedConnection = dataSource.getConnection();

        assertEquals(connection, resolvedConnection);
        verify(statement).execute("SET search_path TO tenant_financruz, public");
    }
}
