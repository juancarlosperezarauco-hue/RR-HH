package com.hrcoresystem.hrcore_api.modules.platform.tenants.application.usecase;

import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto.PlatformTenantResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.mapper.PlatformTenantMapper;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.exception.PlatformTenantNotFoundException;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenantStatus;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetTenantByIdUseCaseTest {

    private PlatformTenantRepository platformTenantRepository;
    private PlatformTenantMapper platformTenantMapper;
    private GetTenantByIdUseCase getTenantByIdUseCase;

    @BeforeEach
    void setUp() {
        platformTenantRepository = mock(PlatformTenantRepository.class);
        platformTenantMapper = new PlatformTenantMapper();
        getTenantByIdUseCase = new GetTenantByIdUseCase(platformTenantRepository, platformTenantMapper);
    }

    @Test
    @DisplayName("Debe retornar el detalle de la organización con sus datos fiscales y de propietario")
    void shouldReturnTenantDetailSuccessfully() {
        UUID tenantId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-03-01T12:00:00Z");

        PlatformTenant tenant = new PlatformTenant(
                tenantId,
                "Farmacias Global",
                "farmacias-global",
                "tenant_farmacias_global",
                PlatformTenantStatus.ACTIVE,
                planId,
                "cus_farm_111",
                true,
                "Farmacias Global S.A.",
                "8899776655",
                "contacto@farmaciasglobal.com",
                "+591-70011223",
                "Ana Torrez",
                "ana.torrez@farmaciasglobal.com",
                createdAt,
                createdAt
        );

        when(platformTenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        PlatformTenantResponse response = getTenantByIdUseCase.execute(tenantId);

        assertNotNull(response);
        assertEquals(tenantId, response.id());
        assertEquals("Farmacias Global", response.name());
        assertEquals("Farmacias Global S.A.", response.legalName());
        assertEquals("8899776655", response.taxIdentifier());
        assertEquals("Ana Torrez", response.ownerName());
        assertEquals("ana.torrez@farmaciasglobal.com", response.ownerEmail());
        assertEquals("ACTIVE", response.status());
        assertTrue(response.active());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando la organización no existe por id")
    void shouldThrowExceptionWhenTenantNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(platformTenantRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(PlatformTenantNotFoundException.class, () -> getTenantByIdUseCase.execute(nonExistentId));
    }
}
