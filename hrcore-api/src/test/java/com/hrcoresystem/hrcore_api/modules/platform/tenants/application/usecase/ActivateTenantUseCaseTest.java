package com.hrcoresystem.hrcore_api.modules.platform.tenants.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto.PlatformTenantResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.mapper.PlatformTenantMapper;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.exception.PlatformTenantNotFoundException;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenantStatus;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ActivateTenantUseCaseTest {

    private PlatformTenantRepository platformTenantRepository;
    private PlatformTenantMapper platformTenantMapper;
    private AuditTrailService auditTrailService;
    private ActivateTenantUseCase activateTenantUseCase;

    @BeforeEach
    void setUp() {
        platformTenantRepository = mock(PlatformTenantRepository.class);
        platformTenantMapper = new PlatformTenantMapper();
        auditTrailService = mock(AuditTrailService.class);
        activateTenantUseCase = new ActivateTenantUseCase(
                platformTenantRepository,
                platformTenantMapper,
                auditTrailService
        );
    }

    @Test
    @DisplayName("Debe activar exitosamente una organización inactiva, conservando sus datos y registrando auditoría")
    void shouldActivateTenantSuccessfully() {
        UUID tenantId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        Instant now = Instant.now();

        PlatformTenant inactiveTenant = new PlatformTenant(
                tenantId,
                "Corporacion Alpha",
                "alpha-corp",
                "tenant_alpha_corp",
                PlatformTenantStatus.INACTIVE,
                planId,
                "cus_alpha_123",
                false,
                "Corporacion Alpha S.A.",
                "1029384756",
                "contacto@alphacorp.com",
                "+591-77112233",
                "Carlos Gomez",
                "carlos@alphacorp.com",
                now,
                now
        );

        when(platformTenantRepository.findById(tenantId)).thenReturn(Optional.of(inactiveTenant));
        when(platformTenantRepository.save(any(PlatformTenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlatformTenantResponse response = activateTenantUseCase.execute(tenantId);

        assertNotNull(response);
        assertEquals(tenantId, response.id());
        assertEquals("ACTIVE", response.status());
        assertTrue(response.active());
        assertEquals("Corporacion Alpha S.A.", response.legalName());
        assertEquals("1029384756", response.taxIdentifier());
        assertEquals("Carlos Gomez", response.ownerName());
        assertEquals("carlos@alphacorp.com", response.ownerEmail());

        ArgumentCaptor<PlatformTenant> captor = ArgumentCaptor.forClass(PlatformTenant.class);
        verify(platformTenantRepository).save(captor.capture());
        PlatformTenant savedTenant = captor.getValue();
        assertEquals(PlatformTenantStatus.ACTIVE, savedTenant.status());
        assertTrue(savedTenant.active());
        assertEquals("1029384756", savedTenant.taxIdentifier());
        assertEquals("Corporacion Alpha S.A.", savedTenant.legalName());

        verify(auditTrailService).recordPlatformEvent(
                eq(AuditEventTypes.TENANT_ACTIVATED),
                eq("TENANT"),
                eq(tenantId.toString()),
                any(),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando la organización a activar no existe")
    void shouldThrowExceptionWhenTenantNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(platformTenantRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(PlatformTenantNotFoundException.class, () -> activateTenantUseCase.execute(nonExistentId));
        verify(platformTenantRepository, never()).save(any());
        verify(auditTrailService, never()).recordPlatformEvent(any(), any(), any(), any(), any(), any());
    }
}
