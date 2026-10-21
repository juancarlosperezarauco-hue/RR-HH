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

class DeactivateTenantUseCaseTest {

    private PlatformTenantRepository platformTenantRepository;
    private PlatformTenantMapper platformTenantMapper;
    private AuditTrailService auditTrailService;
    private DeactivateTenantUseCase deactivateTenantUseCase;

    @BeforeEach
    void setUp() {
        platformTenantRepository = mock(PlatformTenantRepository.class);
        platformTenantMapper = new PlatformTenantMapper();
        auditTrailService = mock(AuditTrailService.class);
        deactivateTenantUseCase = new DeactivateTenantUseCase(
                platformTenantRepository,
                platformTenantMapper,
                auditTrailService
        );
    }

    @Test
    @DisplayName("Debe desactivar lógicamente la organización, conservar todos sus datos históricos y registrar auditoría")
    void shouldDeactivateTenantSuccessfullyAndPreserveHistoricalData() {
        UUID tenantId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-15T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-15T10:00:00Z");

        PlatformTenant activeTenant = new PlatformTenant(
                tenantId,
                "Industrias Beta",
                "industrias-beta",
                "tenant_industrias_beta",
                PlatformTenantStatus.ACTIVE,
                planId,
                "cus_beta_999",
                true,
                "Industrias Beta S.R.L.",
                "9876543210",
                "rrhh@industriasbeta.com",
                "+591-71234567",
                "Maria Fernandez",
                "maria@industriasbeta.com",
                createdAt,
                updatedAt
        );

        when(platformTenantRepository.findById(tenantId)).thenReturn(Optional.of(activeTenant));
        when(platformTenantRepository.save(any(PlatformTenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlatformTenantResponse response = deactivateTenantUseCase.execute(tenantId);

        assertNotNull(response);
        assertEquals(tenantId, response.id());
        assertEquals("INACTIVE", response.status());
        assertFalse(response.active());

        // Verificar que los datos históricos y fiscales se conservan intactos
        assertEquals("Industrias Beta", response.name());
        assertEquals("industrias-beta", response.slug());
        assertEquals("tenant_industrias_beta", response.schemaName());
        assertEquals("Industrias Beta S.R.L.", response.legalName());
        assertEquals("9876543210", response.taxIdentifier());
        assertEquals("rrhh@industriasbeta.com", response.contactEmail());
        assertEquals("+591-71234567", response.contactPhone());
        assertEquals("Maria Fernandez", response.ownerName());
        assertEquals("maria@industriasbeta.com", response.ownerEmail());
        assertEquals(createdAt, response.createdAt());

        ArgumentCaptor<PlatformTenant> captor = ArgumentCaptor.forClass(PlatformTenant.class);
        verify(platformTenantRepository).save(captor.capture());
        PlatformTenant savedTenant = captor.getValue();
        assertEquals(PlatformTenantStatus.INACTIVE, savedTenant.status());
        assertFalse(savedTenant.active());
        assertEquals("Industrias Beta S.R.L.", savedTenant.legalName());
        assertEquals("9876543210", savedTenant.taxIdentifier());
        assertEquals("Maria Fernandez", savedTenant.ownerName());

        verify(auditTrailService).recordPlatformEvent(
                eq(AuditEventTypes.TENANT_DEACTIVATED),
                eq("TENANT"),
                eq(tenantId.toString()),
                any(),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando la organización a desactivar no existe")
    void shouldThrowExceptionWhenTenantNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(platformTenantRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(PlatformTenantNotFoundException.class, () -> deactivateTenantUseCase.execute(nonExistentId));
        verify(platformTenantRepository, never()).save(any());
        verify(auditTrailService, never()).recordPlatformEvent(any(), any(), any(), any(), any(), any());
    }
}
