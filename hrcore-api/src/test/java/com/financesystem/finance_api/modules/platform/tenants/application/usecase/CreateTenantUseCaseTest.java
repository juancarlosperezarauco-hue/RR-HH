package com.hrcoresystem.hrcore_api.modules.platform.tenants.application.usecase;

import com.hrcoresystem.hrcore_api.bootstrap.tenant.TenantBootstrapService;
import com.hrcoresystem.hrcore_api.common.tenancy.migration.TenantSchemaMigrationService;
import com.hrcoresystem.hrcore_api.common.tenancy.schema.TenantSchemaNamingStrategy;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.PlatformSubscriptionProvisioningService;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto.CreateTenantRequest;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto.PlatformTenantResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.mapper.PlatformTenantMapper;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenantStatus;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CreateTenantUseCaseTest {

    @Test
    void shouldCreateTenantAndProvisionSubscriptionSuccessfully() {
        PlatformTenantRepository tenantRepository = mock(PlatformTenantRepository.class);
        TenantSchemaNamingStrategy namingStrategy = mock(TenantSchemaNamingStrategy.class);
        TenantSchemaMigrationService migrationService = mock(TenantSchemaMigrationService.class);
        TenantBootstrapService bootstrapService = mock(TenantBootstrapService.class);
        PlatformTenantMapper mapper = mock(PlatformTenantMapper.class);
        PlatformSubscriptionProvisioningService subscriptionProvisioningService = mock(PlatformSubscriptionProvisioningService.class);

        CreateTenantUseCase useCase = new CreateTenantUseCase(
                tenantRepository,
                namingStrategy,
                migrationService,
                bootstrapService,
                mapper,
                subscriptionProvisioningService,
                mock(com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService.class)
        );

        CreateTenantRequest request = new CreateTenantRequest("FinanCruz Ltda", "financruz", "DEMO");

        UUID tenantId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        Instant now = Instant.now();

        PlatformTenant createdTenant = new PlatformTenant(
                tenantId,
                "FinanCruz Ltda",
                "financruz",
                "tenant_financruz",
                PlatformTenantStatus.ACTIVE,
                null,
                true,
                now,
                now
        );

        PlatformTenant updatedTenant = new PlatformTenant(
                tenantId,
                "FinanCruz Ltda",
                "financruz",
                "tenant_financruz",
                PlatformTenantStatus.ACTIVE,
                planId,
                true,
                now,
                now
        );

        PlatformTenantResponse expectedResponse = new PlatformTenantResponse(
                tenantId,
                "FinanCruz Ltda",
                "financruz",
                "tenant_financruz",
                "ACTIVE",
                planId,
                null,
                true,
                now,
                now
        );

        when(namingStrategy.normalizeSlug("financruz")).thenReturn("financruz");
        when(namingStrategy.toSchemaName("financruz")).thenReturn("tenant_financruz");
        when(tenantRepository.existsBySlug("financruz")).thenReturn(false);
        when(tenantRepository.existsBySchemaName("tenant_financruz")).thenReturn(false);
        when(tenantRepository.save(any())).thenReturn(createdTenant);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(updatedTenant));
        when(mapper.toResponse(updatedTenant)).thenReturn(expectedResponse);

        PlatformTenantResponse actualResponse = useCase.execute(request);

        assertEquals(expectedResponse, actualResponse);
        verify(migrationService).migrateSchema("tenant_financruz");
        verify(bootstrapService).initializeTenantData("tenant_financruz", "FinanCruz Ltda");
        verify(subscriptionProvisioningService).assignCurrentSubscription(tenantId, "DEMO", null, true);
    }
}
