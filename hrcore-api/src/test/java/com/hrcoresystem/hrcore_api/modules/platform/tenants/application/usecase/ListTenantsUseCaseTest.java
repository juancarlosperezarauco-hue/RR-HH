package com.hrcoresystem.hrcore_api.modules.platform.tenants.application.usecase;

import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto.PlatformTenantFilterCriteria;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto.PlatformTenantResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.mapper.PlatformTenantMapper;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenantStatus;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ListTenantsUseCaseTest {

    private PlatformTenantRepository platformTenantRepository;
    private PlatformTenantMapper platformTenantMapper;
    private PlatformPlanRepository platformPlanRepository;
    private ListTenantsUseCase listTenantsUseCase;

    @BeforeEach
    void setUp() {
        platformTenantRepository = mock(PlatformTenantRepository.class);
        platformTenantMapper = new PlatformTenantMapper();
        platformPlanRepository = mock(PlatformPlanRepository.class);
        listTenantsUseCase = new ListTenantsUseCase(
                platformTenantRepository,
                platformTenantMapper,
                platformPlanRepository
        );
    }

    @Test
    @DisplayName("Debe listar organizaciones aplicando filtros por nombre, NIT, plan y estado operativo")
    void shouldFilterTenantsByNameNitPlanAndStatus() {
        UUID planId = UUID.randomUUID();
        PlatformPlan plan = new PlatformPlan(
                planId,
                "ENTERPRISE",
                "Plan Empresarial",
                "Plan para grandes empresas",
                500,
                50,
                "ENTERPRISE",
                null,
                true,
                Instant.now(),
                Instant.now()
        );

        when(platformPlanRepository.findByCode("ENTERPRISE")).thenReturn(Optional.of(plan));

        PlatformTenant tenant = new PlatformTenant(
                UUID.randomUUID(),
                "Telecom Bolivia",
                "telecom-bo",
                "tenant_telecom_bo",
                PlatformTenantStatus.ACTIVE,
                planId,
                "cus_telecom",
                true,
                "Telecom Bolivia S.A.",
                "1020304050",
                "soporte@telecom.bo",
                "+591-22334455",
                "Roberto Mendez",
                "roberto@telecom.bo",
                Instant.now(),
                Instant.now()
        );

        when(platformTenantRepository.searchTenants(
                eq("telecom"),
                isNull(),
                eq("1020304050"),
                eq(PlatformTenantStatus.ACTIVE),
                eq(true),
                eq(planId)
        )).thenReturn(List.of(tenant));

        PlatformTenantFilterCriteria criteria = new PlatformTenantFilterCriteria(
                "telecom",
                null,
                "1020304050",
                "ENTERPRISE",
                PlatformTenantStatus.ACTIVE,
                true
        );

        List<PlatformTenantResponse> result = listTenantsUseCase.execute(criteria);

        assertEquals(1, result.size());
        PlatformTenantResponse item = result.getFirst();
        assertEquals("Telecom Bolivia", item.name());
        assertEquals("1020304050", item.taxIdentifier());
        assertEquals("ACTIVE", item.status());
        assertTrue(item.active());
        assertEquals("Roberto Mendez", item.ownerName());
    }

    @Test
    @DisplayName("Debe listar todas las organizaciones cuando no se especifican filtros")
    void shouldListAllTenantsWhenNoFiltersProvided() {
        PlatformTenant tenant1 = new PlatformTenant(
                UUID.randomUUID(),
                "Org Uno",
                "org-uno",
                "tenant_org_uno",
                PlatformTenantStatus.ACTIVE,
                UUID.randomUUID(),
                null,
                true,
                Instant.now(),
                Instant.now()
        );

        when(platformTenantRepository.searchTenants(isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(tenant1));

        List<PlatformTenantResponse> result = listTenantsUseCase.execute();

        assertEquals(1, result.size());
        assertEquals("Org Uno", result.getFirst().name());
    }
}
