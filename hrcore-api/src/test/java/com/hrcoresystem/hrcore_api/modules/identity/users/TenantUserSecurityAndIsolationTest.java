package com.hrcoresystem.hrcore_api.modules.identity.users;

import com.hrcoresystem.hrcore_api.common.security.authorization.domain.model.AuthorizationCatalogState;
import com.hrcoresystem.hrcore_api.common.security.authorization.domain.model.TenantAuthorizationState;
import com.hrcoresystem.hrcore_api.common.security.authorization.domain.repository.AuthorizationCatalogStateRepository;
import com.hrcoresystem.hrcore_api.common.security.authorization.domain.repository.TenantAuthorizationStateRepository;
import com.hrcoresystem.hrcore_api.common.security.principal.AuthenticatedUserPrincipal;
import com.hrcoresystem.hrcore_api.common.security.session.AuthSessionService;
import com.hrcoresystem.hrcore_api.common.security.session.AuthenticatedSessionValidator;
import com.hrcoresystem.hrcore_api.common.security.session.domain.model.AuthSession;
import com.hrcoresystem.hrcore_api.common.security.session.domain.repository.PlatformAuthSessionRepository;
import com.hrcoresystem.hrcore_api.common.security.session.domain.repository.TenantAuthSessionRepository;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto.LoginRequest;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.usecase.LoginTenantUserUseCase;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.exception.AuthenticationFailedException;
import com.hrcoresystem.hrcore_api.modules.identity.users.application.usecase.GetTenantUserByIdUseCase;
import com.hrcoresystem.hrcore_api.modules.identity.users.application.usecase.UpdateTenantUserUseCase;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.exception.TenantUserNotFoundException;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUserStatus;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantUserRoleRepository;
import com.hrcoresystem.hrcore_api.modules.platform.superadmin.domain.repository.PlatformSuperadminRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantUserSecurityAndIsolationTest {

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Criterio 6: Impide que un usuario desactivado inicie sesión")
    void shouldPreventDeactivatedUserFromLoggingIn() {
        TenantUserRepository tenantUserRepository = mock(TenantUserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuthSessionService sessions = mock(AuthSessionService.class);
        AuditTrailService audit = mock(AuditTrailService.class);
        TenantUserRoleRepository roleRepository = mock(TenantUserRoleRepository.class);

        LoginTenantUserUseCase loginUseCase = new LoginTenantUserUseCase(
                tenantUserRepository,
                passwordEncoder,
                sessions,
                audit,
                roleRepository
        );

        TenantContextHolder.set(new TenantContext("empresa-a", "tenant_empresa_a", false));

        TenantUser deactivatedUser = new TenantUser(
                UUID.randomUUID(),
                "suspendido@empresa.com",
                "hashed",
                "Juan",
                "Suspendido",
                false, // inactivo
                TenantUserStatus.INACTIVE,
                1,
                Instant.now(),
                Instant.now()
        );

        when(tenantUserRepository.findByEmail("suspendido@empresa.com")).thenReturn(Optional.of(deactivatedUser));

        LoginRequest request = new LoginRequest("suspendido@empresa.com", "Password123!");

        AuthenticationFailedException ex = assertThrows(AuthenticationFailedException.class, () -> {
            loginUseCase.execute(request);
        });

        assertEquals("User is inactive", ex.getMessage());
    }

    @Test
    @DisplayName("Criterio 6: Impide que un usuario desactivado ejecute operaciones (sesión inválida / stale)")
    void shouldPreventDeactivatedUserFromExecutingOperations() {
        TenantAuthSessionRepository tenantSessions = mock(TenantAuthSessionRepository.class);
        PlatformAuthSessionRepository platformSessions = mock(PlatformAuthSessionRepository.class);
        TenantUserRepository users = mock(TenantUserRepository.class);
        PlatformSuperadminRepository superadmins = mock(PlatformSuperadminRepository.class);
        TenantAuthorizationStateRepository tenantAuth = mock(TenantAuthorizationStateRepository.class);
        AuthorizationCatalogStateRepository catalog = mock(AuthorizationCatalogStateRepository.class);

        AuthenticatedSessionValidator validator = new AuthenticatedSessionValidator(
                tenantSessions,
                platformSessions,
                users,
                superadmins,
                tenantAuth,
                catalog
        );

        TenantContextHolder.set(new TenantContext("empresa-a", "tenant_empresa_a", false));

        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                userId,
                sessionId,
                "user@empresa.com",
                "User",
                "empresa-a",
                "TENANT",
                List.of("ROLE_USER"),
                List.of(),
                1L,
                1L,
                1L
        );

        AuthSession activeSession = new AuthSession(
                sessionId,
                userId,
                UUID.randomUUID(),
                "hash",
                1L,
                Instant.now().minusSeconds(10),
                Instant.now().plusSeconds(3600),
                Instant.now().minusSeconds(10),
                null,
                null,
                "agent",
                "127.0.0.1"
        );

        when(tenantSessions.findById(sessionId)).thenReturn(Optional.of(activeSession));

        // El usuario en la base de datos ahora está desactivado (active = false)
        TenantUser deactivatedUserInDb = new TenantUser(
                userId,
                "user@empresa.com",
                "hashed",
                "User",
                "Deactivated",
                false,
                TenantUserStatus.INACTIVE,
                2L, // versión incrementada
                Instant.now(),
                Instant.now()
        );
        when(users.findById(userId)).thenReturn(Optional.of(deactivatedUserInDb));

        when(tenantAuth.find()).thenReturn(Optional.of(new TenantAuthorizationState((short) 1, userId, 1L, Instant.now())));
        when(catalog.find()).thenReturn(Optional.of(new AuthorizationCatalogState((short) 1, 1L, Instant.now())));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> {
            validator.validate(principal);
        });

        assertEquals("Authorization is stale", ex.getMessage());
    }

    @Test
    @DisplayName("Criterio 8: Impide consultar o modificar usuarios pertenecientes a otros tenants")
    void shouldEnforceTenantIsolationAndPreventCrossTenantAccess() {
        TenantUserRepository tenantUserRepository = mock(TenantUserRepository.class);
        GetTenantUserByIdUseCase getByIdUseCase = new GetTenantUserByIdUseCase(tenantUserRepository, new com.hrcoresystem.hrcore_api.modules.identity.users.application.mapper.TenantUserMapper(null));

        // El contexto actual está en tenant "empresa-a"
        TenantContextHolder.set(new TenantContext("empresa-a", "tenant_empresa_a", false));

        UUID otherTenantUserId = UUID.randomUUID();
        // En el repositorio del tenant actual, ese ID NO existe
        when(tenantUserRepository.findById(otherTenantUserId)).thenReturn(Optional.empty());

        assertThrows(TenantUserNotFoundException.class, () -> {
            getByIdUseCase.execute(otherTenantUserId);
        });

        // Verificamos también que si el token indica un tenant pero la petición va a otro tenant, se rechaza
        TenantAuthSessionRepository tenantSessions = mock(TenantAuthSessionRepository.class);
        PlatformAuthSessionRepository platformSessions = mock(PlatformAuthSessionRepository.class);
        PlatformSuperadminRepository superadmins = mock(PlatformSuperadminRepository.class);
        TenantAuthorizationStateRepository tenantAuth = mock(TenantAuthorizationStateRepository.class);
        AuthorizationCatalogStateRepository catalog = mock(AuthorizationCatalogStateRepository.class);

        AuthenticatedSessionValidator validator = new AuthenticatedSessionValidator(
                tenantSessions,
                platformSessions,
                tenantUserRepository,
                superadmins,
                tenantAuth,
                catalog
        );

        AuthenticatedUserPrincipal principalFromOtherTenant = new AuthenticatedUserPrincipal(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "attacker@tenant-b.com",
                "Attacker",
                "tenant-b", // El token pertenece a tenant-b
                "TENANT",
                List.of("ROLE_ADMIN"),
                List.of(),
                1L,
                1L,
                1L
        );

        BadCredentialsException scopeEx = assertThrows(BadCredentialsException.class, () -> {
            validator.validate(principalFromOtherTenant);
        });

        assertEquals("Invalid tenant scope", scopeEx.getMessage());
    }
}
