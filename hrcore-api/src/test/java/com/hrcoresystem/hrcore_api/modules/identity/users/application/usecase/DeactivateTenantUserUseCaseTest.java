package com.hrcoresystem.hrcore_api.modules.identity.users.application.usecase;

import com.hrcoresystem.hrcore_api.common.security.authorization.domain.repository.TenantAuthorizationStateRepository;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.common.security.session.domain.repository.TenantAuthSessionRepository;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.identity.users.application.dto.TenantUserResponse;
import com.hrcoresystem.hrcore_api.modules.identity.users.application.mapper.TenantUserMapper;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUserStatus;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeactivateTenantUserUseCaseTest {

    private TenantUserRepository tenantUserRepository;
    private TenantUserMapper tenantUserMapper;
    private AuditTrailService auditTrailService;
    private TenantAuthorizationStateRepository authorizationStateRepository;
    private TenantAuthSessionRepository sessions;
    private SecurityContextFacade securityContextFacade;

    private DeactivateTenantUserUseCase useCase;

    @BeforeEach
    void setUp() {
        tenantUserRepository = mock(TenantUserRepository.class);
        tenantUserMapper = new TenantUserMapper(null);
        auditTrailService = mock(AuditTrailService.class);
        authorizationStateRepository = mock(TenantAuthorizationStateRepository.class);
        sessions = mock(TenantAuthSessionRepository.class);
        securityContextFacade = mock(SecurityContextFacade.class);

        useCase = new DeactivateTenantUserUseCase(
                tenantUserRepository,
                tenantUserMapper,
                auditTrailService,
                authorizationStateRepository,
                sessions,
                securityContextFacade
        );
    }

    @Test
    @DisplayName("Criterio 5: Desactiva el acceso de un usuario, revoca sus sesiones e incrementa versión de seguridad")
    void shouldDeactivateUserRevokeSessionsAndIncrementSecurityVersion() {
        UUID targetUserId = UUID.randomUUID();
        UUID callerUserId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();

        when(authorizationStateRepository.ownerUserId()).thenReturn(ownerUserId);
        when(securityContextFacade.getCurrentUserId()).thenReturn(callerUserId);

        TenantUser user = new TenantUser(
                targetUserId,
                "user@empresa.com",
                "hashed",
                "Juan",
                "Pérez",
                true,
                TenantUserStatus.ACTIVE,
                1,
                Instant.now(),
                Instant.now()
        );

        when(tenantUserRepository.findById(targetUserId)).thenReturn(Optional.of(user));
        when(tenantUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TenantUserResponse response = useCase.execute(targetUserId);

        assertFalse(response.active());
        assertEquals("INACTIVE", response.status());

        verify(sessions).revokeAllByUserId(targetUserId);
        verify(tenantUserRepository).save(argThat(u -> !u.active() && u.securityVersion() == 2));
    }

    @Test
    @DisplayName("Criterio 7: Bloquea la autodesactivación de la propia cuenta de administrador o propietario")
    void shouldBlockSelfDeactivation() {
        UUID callerAndTargetId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();

        when(authorizationStateRepository.ownerUserId()).thenReturn(ownerUserId);
        when(securityContextFacade.getCurrentUserId()).thenReturn(callerAndTargetId);

        TenantUser user = new TenantUser(
                callerAndTargetId,
                "admin@empresa.com",
                "hashed",
                "Admin",
                "Activo",
                true,
                TenantUserStatus.ACTIVE,
                1,
                Instant.now(),
                Instant.now()
        );

        when(tenantUserRepository.findById(callerAndTargetId)).thenReturn(Optional.of(user));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> useCase.execute(callerAndTargetId));
        assertTrue(ex.getMessage().contains("desactivar tu propia cuenta"));
        verify(tenantUserRepository, never()).save(any());
        verify(sessions, never()).revokeAllByUserId(any());
    }

    @Test
    @DisplayName("Criterio 7: Bloquea la desactivación del propietario del tenant")
    void shouldBlockDeactivationOfTenantOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();

        when(authorizationStateRepository.ownerUserId()).thenReturn(ownerId);
        when(securityContextFacade.getCurrentUserId()).thenReturn(callerId);

        TenantUser ownerUser = new TenantUser(
                ownerId,
                "owner@empresa.com",
                "hashed",
                "Owner",
                "Tenant",
                true,
                TenantUserStatus.ACTIVE,
                1,
                Instant.now(),
                Instant.now()
        );

        when(tenantUserRepository.findById(ownerId)).thenReturn(Optional.of(ownerUser));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> useCase.execute(ownerId));
        assertTrue(ex.getMessage().contains("tenant owner cannot be deactivated"));
        verify(tenantUserRepository, never()).save(any());
    }
}
