package com.hrcoresystem.hrcore_api.modules.identity.auth.application.usecase;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.NotificationPublishRequest;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.port.NotificationPublisherPort;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto.ChangePasswordRequest;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUserStatus;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ChangePasswordUseCaseTest {

    @Test
    void shouldChangePasswordSuccessfully() {
        SecurityContextFacade securityContextFacade = mock(SecurityContextFacade.class);
        TenantUserRepository tenantUserRepository = mock(TenantUserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuditTrailService auditTrailService = mock(AuditTrailService.class);
        NotificationPublisherPort notificationPublisherPort = mock(NotificationPublisherPort.class);
        ObjectMapper objectMapper = new ObjectMapper();

        ChangePasswordUseCase useCase = new ChangePasswordUseCase(
                securityContextFacade,
                tenantUserRepository,
                passwordEncoder,
                auditTrailService,
                notificationPublisherPort,
                objectMapper
        );

        UUID userId = UUID.randomUUID();
        TenantUser tenantUser = new TenantUser(
                userId,
                "admin@financruz.com",
                "old-hash",
                "Admin",
                "Tenant",
                true,
                TenantUserStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );

        when(securityContextFacade.getCurrentSubject()).thenReturn(userId.toString());
        when(tenantUserRepository.findById(userId)).thenReturn(Optional.of(tenantUser));
        when(passwordEncoder.matches("OldPassword123!", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword456!")).thenReturn("new-hash");

        useCase.execute(new ChangePasswordRequest("OldPassword123!", "NewPassword456!"));

        verify(tenantUserRepository).save(any(TenantUser.class));
        verify(notificationPublisherPort).publish(any(NotificationPublishRequest.class));
        verify(auditTrailService).recordTenantEvent(
                anyString(),
                eq("USER"),
                eq(tenantUser.id().toString()),
                any(),
                any(),
                any()
        );
    }

    @Test
    void shouldFailWhenNewPasswordEqualsCurrentPassword() {
        SecurityContextFacade securityContextFacade = mock(SecurityContextFacade.class);
        TenantUserRepository tenantUserRepository = mock(TenantUserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuditTrailService auditTrailService = mock(AuditTrailService.class);
        NotificationPublisherPort notificationPublisherPort = mock(NotificationPublisherPort.class);
        ObjectMapper objectMapper = new ObjectMapper();

        ChangePasswordUseCase useCase = new ChangePasswordUseCase(
                securityContextFacade,
                tenantUserRepository,
                passwordEncoder,
                auditTrailService,
                notificationPublisherPort,
                objectMapper
        );

        UUID userId = UUID.randomUUID();
        TenantUser tenantUser = new TenantUser(
                userId,
                "admin@financruz.com",
                "old-hash",
                "Admin",
                "Tenant",
                true,
                TenantUserStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );

        when(securityContextFacade.getCurrentSubject()).thenReturn(userId.toString());
        when(tenantUserRepository.findById(userId)).thenReturn(Optional.of(tenantUser));
        when(passwordEncoder.matches("SamePassword123!", "old-hash")).thenReturn(true);

        assertThrows(BusinessException.class, () ->
                useCase.execute(new ChangePasswordRequest("SamePassword123!", "SamePassword123!"))
        );
    }
}
