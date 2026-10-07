package com.hrcoresystem.hrcore_api.modules.platform.tenants.application.usecase;

import com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.service.TenantOwnerAdminProvisioningService;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto.CreatePlatformTenantRequest;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto.PlatformTenantResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreatePlatformTenantUseCaseTest {

    @Test
    void shouldCreateTenantAndProvisionOwnerAdmin() {
        CreateTenantUseCase createTenantUseCase = mock(CreateTenantUseCase.class);
        TenantOwnerAdminProvisioningService tenantOwnerAdminProvisioningService = mock(TenantOwnerAdminProvisioningService.class);

        CreatePlatformTenantUseCase useCase = new CreatePlatformTenantUseCase(
                createTenantUseCase,
                tenantOwnerAdminProvisioningService
        );

        UUID tenantId = UUID.randomUUID();
        Instant now = Instant.now();

        CreatePlatformTenantRequest request = new CreatePlatformTenantRequest(
                "FinanCruz Ltda",
                "financruz",
                "DEMO",
                "admin@financruz.com",
                "Password123!",
                "Carlos",
                "Rojas"
        );

        PlatformTenantResponse createdTenantResponse = new PlatformTenantResponse(
                tenantId,
                "FinanCruz Ltda",
                "financruz",
                "tenant_financruz",
                "ACTIVE",
                UUID.randomUUID(),
                null,
                true,
                now,
                now
        );

        when(createTenantUseCase.execute(any())).thenReturn(createdTenantResponse);

        PlatformTenantResponse actualResponse = useCase.execute(request);

        assertEquals(createdTenantResponse, actualResponse);
        verify(createTenantUseCase).execute(any());
        verify(tenantOwnerAdminProvisioningService).provisionOwnerAdmin(
                "tenant_financruz",
                "financruz",
                "admin@financruz.com",
                "Password123!",
                "Carlos",
                "Rojas"
        );
    }
}
