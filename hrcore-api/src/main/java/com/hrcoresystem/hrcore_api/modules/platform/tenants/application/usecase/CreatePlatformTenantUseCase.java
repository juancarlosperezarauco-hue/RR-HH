package com.hrcoresystem.hrcore_api.modules.platform.tenants.application.usecase;

import com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.service.TenantOwnerAdminProvisioningService;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto.CreatePlatformTenantRequest;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto.CreateTenantRequest;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto.PlatformTenantResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreatePlatformTenantUseCase {

    private final CreateTenantUseCase createTenantUseCase;
    private final TenantOwnerAdminProvisioningService tenantOwnerAdminProvisioningService;

    public CreatePlatformTenantUseCase(
            CreateTenantUseCase createTenantUseCase,
            TenantOwnerAdminProvisioningService tenantOwnerAdminProvisioningService
    ) {
        this.createTenantUseCase = createTenantUseCase;
        this.tenantOwnerAdminProvisioningService = tenantOwnerAdminProvisioningService;
    }

    @Transactional
    public PlatformTenantResponse execute(CreatePlatformTenantRequest request) {
        PlatformTenantResponse tenantResponse = createTenantUseCase.execute(
                new CreateTenantRequest(request.name(), request.slug(), request.planCode())
        );

        tenantOwnerAdminProvisioningService.provisionOwnerAdmin(
                tenantResponse.schemaName(),
                tenantResponse.slug(),
                request.adminEmail(),
                request.password(),
                request.firstName(),
                request.lastName()
        );

        return tenantResponse;
    }
}
