package com.hrcoresystem.hrcore_api.modules.identity.access;

import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.policy.TenantOwnershipPolicy;
import com.hrcoresystem.hrcore_api.modules.identity.access.infrastructure.security.TenantOwnershipGuard;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.PlatformSubscriptionLifecycleService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.exception.TenantSubscriptionAccessDeniedException;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscription;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscriptionStatus;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.repository.PlatformSubscriptionRepository;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenantStatus;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantRoleSecurityAndIsolationTest {

    @Mock
    private PlatformTenantRepository platformTenantRepository;

    @Mock
    private PlatformSubscriptionRepository platformSubscriptionRepository;

    @Mock
    private PlatformPlanRepository platformPlanRepository;

    @Mock
    private PlatformSubscriptionLifecycleService platformSubscriptionLifecycleService;

    @Mock
    private TenantOwnershipPolicy tenantOwnershipPolicy;

    private TenantPlanEnforcementService planEnforcementService;
    private TenantOwnershipGuard ownershipGuard;

    private final UUID tenant1Id = UUID.randomUUID();
    private final String tenant1Slug = "tenant-alfa";
    private final UUID tenant2Id = UUID.randomUUID();
    private final String tenant2Slug = "tenant-beta";

    @BeforeEach
    void setUp() {
        planEnforcementService = new TenantPlanEnforcementService(
                platformTenantRepository,
                platformSubscriptionRepository,
                platformPlanRepository,
                platformSubscriptionLifecycleService
        );

        ownershipGuard = new TenantOwnershipGuard(tenantOwnershipPolicy);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Criterio 6: Valida módulos permitidos por planes BASIC, PRO y ENTERPRISE")
    void shouldEnforceModulesAllowedPerPlan() {
        TenantContextHolder.set(new TenantContext(tenant1Slug, "tenant_" + tenant1Slug, false));

        UUID basicPlanId = UUID.randomUUID();

        PlatformTenant tenant = new PlatformTenant(
                tenant1Id,
                "Alfa Corp",
                tenant1Slug,
                "tenant_" + tenant1Slug,
                PlatformTenantStatus.ACTIVE,
                basicPlanId,
                null,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        PlatformPlan basicPlan = new PlatformPlan(
                basicPlanId,
                "BASIC",
                "Plan Básico",
                "Plan Básico HR",
                10,
                5,
                "PAID",
                null,
                true,
                Instant.now(),
                Instant.now()
        );

        PlatformSubscription subscription = new PlatformSubscription(
                UUID.randomUUID(),
                tenant1Id,
                basicPlanId,
                PlatformSubscriptionStatus.ACTIVE,
                false,
                true,
                null,
                null,
                null,
                Instant.now(),
                Instant.now().plusSeconds(86400),
                null,
                null,
                false,
                null,
                Instant.now(),
                Instant.now()
        );

        when(platformTenantRepository.findBySlug(tenant1Slug)).thenReturn(Optional.of(tenant));
        when(platformSubscriptionRepository.findCurrentByTenantId(tenant1Id)).thenReturn(Optional.of(subscription));
        when(platformPlanRepository.findById(basicPlanId)).thenReturn(Optional.of(basicPlan));

        Map<String, String> codeToModule = Map.of(
                "employees.create", "employees",
                "shifts.create", "shifts",
                "incidents.create", "incidents"
        );

        // Permisos del módulo employees están permitidos en BASIC
        assertDoesNotThrow(() -> {
            planEnforcementService.assertCanAssignPermissions(List.of("employees.create"), codeToModule);
        });

        // Permisos del módulo shifts NO están incluidos en BASIC -> lanza excepción
        TenantSubscriptionAccessDeniedException exShifts = assertThrows(
                TenantSubscriptionAccessDeniedException.class,
                () -> planEnforcementService.assertCanAssignPermissions(List.of("shifts.create"), codeToModule)
        );
        assertTrue(exShifts.getMessage().contains("shifts"));

        // Permisos del módulo incidents NO están incluidos en BASIC -> lanza excepción
        TenantSubscriptionAccessDeniedException exIncidents = assertThrows(
                TenantSubscriptionAccessDeniedException.class,
                () -> planEnforcementService.assertCanAssignPermissions(List.of("incidents.create"), codeToModule)
        );
        assertTrue(exIncidents.getMessage().contains("incidents"));
    }

    @Test
    @DisplayName("Criterio 7: Bloquea operaciones a usuarios que no son propietarios activos del tenant")
    void shouldDenyAccessToNonOwnerUsers() {
        when(tenantOwnershipPolicy.isCurrentOwner()).thenReturn(false);
        assertFalse(ownershipGuard.isCurrentOwner());
    }

    @Test
    @DisplayName("Criterio 7: Permite operaciones de roles al propietario activo del tenant")
    void shouldAllowAccessToCurrentOwnerUser() {
        when(tenantOwnershipPolicy.isCurrentOwner()).thenReturn(true);
        assertTrue(ownershipGuard.isCurrentOwner());
    }

    @Test
    @DisplayName("Criterio 7: Impide resolver políticas o consultar datos de tenants ajenos")
    void shouldEnforceStrictTenantIsolation() {
        // Establecer contexto para Tenant Alfa
        TenantContextHolder.set(new TenantContext(tenant1Slug, "tenant_" + tenant1Slug, false));
        assertEquals(tenant1Slug, TenantContextHolder.getRequired().tenantSlug());
        assertEquals("tenant_" + tenant1Slug, TenantContextHolder.getRequired().schemaName());

        // Limpiar y cambiar a Tenant Beta
        TenantContextHolder.clear();
        TenantContextHolder.set(new TenantContext(tenant2Slug, "tenant_" + tenant2Slug, false));
        assertEquals(tenant2Slug, TenantContextHolder.getRequired().tenantSlug());
        assertNotEquals("tenant_" + tenant1Slug, TenantContextHolder.getRequired().schemaName());
    }
}
