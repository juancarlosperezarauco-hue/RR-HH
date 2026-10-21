package com.hrcoresystem.hrcore_api.modules.platform.tenants;

import com.hrcoresystem.hrcore_api.common.security.authorization.AuthorizationGuards;
import com.hrcoresystem.hrcore_api.common.security.principal.AuthenticatedUserPrincipal;
import com.hrcoresystem.hrcore_api.common.tenancy.TenancyProperties;
import com.hrcoresystem.hrcore_api.common.tenancy.exception.TenantResolutionException;
import com.hrcoresystem.hrcore_api.common.tenancy.resolver.HeaderTenantResolver;
import com.hrcoresystem.hrcore_api.common.tenancy.schema.TenantSchemaNamingStrategy;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class OrganizationManagementSecurityAndOperationalTest {

    @Test
    @DisplayName("Criterio 1: isPlatformAdmin valida que el usuario sea Superadministrador en el scope PLATFORM")
    void shouldValidateSuperadminPrivileges() {
        AuthorizationGuards guards = new AuthorizationGuards();

        // 1. Sin autenticación -> Rechazado
        SecurityContextHolder.clearContext();
        assertFalse(guards.isPlatformAdmin(), "Debe rechazar si no hay autenticación");

        // 2. Autenticado como usuario regular de tenant -> Rechazado
        AuthenticatedUserPrincipal tenantUserPrincipal = new AuthenticatedUserPrincipal(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "user@empresa.com",
                "Usuario Tenant",
                "empresa",
                "TENANT",
                List.of("ROLE_USER"),
                List.of(),
                1L,
                1L,
                1L
        );
        Authentication tenantAuth = mock(Authentication.class);
        when(tenantAuth.isAuthenticated()).thenReturn(true);
        when(tenantAuth.getPrincipal()).thenReturn(tenantUserPrincipal);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(tenantAuth).getAuthorities();

        SecurityContextHolder.setContext(new SecurityContextImpl(tenantAuth));
        assertFalse(guards.isPlatformAdmin(), "Debe rechazar a un usuario con scope TENANT");

        // 3. Autenticado con scope PLATFORM pero sin rol ROLE_SUPERADMIN -> Rechazado
        AuthenticatedUserPrincipal platformUserPrincipal = new AuthenticatedUserPrincipal(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "support@hrcore.com",
                "Support User",
                "platform",
                "PLATFORM",
                List.of("ROLE_SUPPORT"),
                List.of(),
                1L,
                1L,
                1L
        );
        Authentication platformAuthWithoutRole = mock(Authentication.class);
        when(platformAuthWithoutRole.isAuthenticated()).thenReturn(true);
        when(platformAuthWithoutRole.getPrincipal()).thenReturn(platformUserPrincipal);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_SUPPORT"))).when(platformAuthWithoutRole).getAuthorities();

        SecurityContextHolder.setContext(new SecurityContextImpl(platformAuthWithoutRole));
        assertFalse(guards.isPlatformAdmin(), "Debe rechazar si no tiene el rol ROLE_SUPERADMIN");

        // 4. Autenticado como Superadministrador de plataforma -> Permitido
        AuthenticatedUserPrincipal superadminPrincipal = new AuthenticatedUserPrincipal(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "superadmin@hrcore.com",
                "Super Admin",
                "platform",
                "PLATFORM",
                List.of("ROLE_SUPERADMIN"),
                List.of(),
                1L,
                1L,
                1L
        );
        Authentication superadminAuth = mock(Authentication.class);
        when(superadminAuth.isAuthenticated()).thenReturn(true);
        when(superadminAuth.getPrincipal()).thenReturn(superadminPrincipal);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_SUPERADMIN"))).when(superadminAuth).getAuthorities();

        SecurityContextHolder.setContext(new SecurityContextImpl(superadminAuth));
        assertTrue(guards.isPlatformAdmin(), "Debe validar exitosamente al Superadministrador");

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Criterio 5: HeaderTenantResolver impide acceder o resolver una organización desactivada")
    void shouldRejectAccessToDeactivatedTenant() throws SQLException {
        TenancyProperties properties = new TenancyProperties();
        properties.setHeaderName("X-Tenant-ID");
        properties.setPublicSchema("public");
        properties.setTenantSchemaPrefix("tenant_");
        properties.setPublicPaths(List.of("/api/public/**"));
        properties.setGlobalPaths(List.of("/api/platform/**"));

        TenantSchemaNamingStrategy namingStrategy = new TenantSchemaNamingStrategy(properties);

        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);

        // Simulamos que el tenant existe en la BD pero active = false y status = "INACTIVE"
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString("schema_name")).thenReturn("tenant_empresa_suspendida");
        when(resultSet.getBoolean("active")).thenReturn(false);
        when(resultSet.getString("status")).thenReturn("INACTIVE");

        HeaderTenantResolver resolver = new HeaderTenantResolver(properties, namingStrategy, dataSource);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/employees");
        when(request.getHeader("X-Tenant-ID")).thenReturn("empresa-suspendida");

        TenantResolutionException exception = assertThrows(TenantResolutionException.class, () -> {
            resolver.resolve(request);
        });

        assertEquals("Tenant is not active", exception.getMessage());
    }
}
