package com.hrcoresystem.hrcore_api.modules.identity.auth.application.usecase;

import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.NotificationPublishRequest;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.config.PasswordResetNotificationProperties;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.usecase.SendPasswordResetNotificationUseCase;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.port.NotificationPublisherPort;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto.ForgotPasswordRequest;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.repository.PasswordResetTokenRepository;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUserStatus;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ForgotPasswordUseCaseTest {

    @AfterEach
    void cleanTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldCreateResetTokenAndSendNotificationWhenUserExists() {
        TenantUserRepository tenantUserRepository = mock(TenantUserRepository.class);
        PasswordResetTokenRepository passwordResetTokenRepository = mock(PasswordResetTokenRepository.class);
        SendPasswordResetNotificationUseCase sendPasswordResetNotificationUseCase = mock(SendPasswordResetNotificationUseCase.class);
        AuditTrailService auditTrailService = mock(AuditTrailService.class);
        NotificationPublisherPort notificationPublisherPort = mock(NotificationPublisherPort.class);
        ObjectMapper objectMapper = new ObjectMapper();

        PasswordResetNotificationProperties properties = new PasswordResetNotificationProperties();
        properties.setExpirationMinutes(30);
        properties.setFrontendUrlBase("http://localhost:4200");
        properties.setResetUrlPath("/reset-password");

        ForgotPasswordUseCase useCase = new ForgotPasswordUseCase(
                tenantUserRepository,
                passwordResetTokenRepository,
                properties,
                sendPasswordResetNotificationUseCase,
                auditTrailService,
                notificationPublisherPort,
                objectMapper
        );

        TenantContextHolder.set(new TenantContext("financruz", "tenant_financruz", false));

        TenantUser tenantUser = new TenantUser(
                UUID.randomUUID(),
                "admin@financruz.com",
                "hash",
                "Admin",
                "Tenant",
                true,
                TenantUserStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );

        when(tenantUserRepository.findByEmail("admin@financruz.com")).thenReturn(Optional.of(tenantUser));

        useCase.execute(new ForgotPasswordRequest("admin@financruz.com"));

        verify(passwordResetTokenRepository).invalidateAllByEmail(eq("admin@financruz.com"), any());
        verify(passwordResetTokenRepository).save(any());
        verify(sendPasswordResetNotificationUseCase).execute(any());
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
}
