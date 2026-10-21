package com.hrcoresystem.hrcore_api.modules.platform.tenants.application.usecase;

import com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.service.TenantOwnerAdminProvisioningService;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto.CreatePlatformTenantRequest;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto.CreateTenantRequest;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto.PlatformTenantResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.mapper.PlatformTenantMapper;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreatePlatformTenantUseCase {

    private final CreateTenantUseCase createTenantUseCase;
    private final TenantOwnerAdminProvisioningService tenantOwnerAdminProvisioningService;
    private final PlatformTenantRepository platformTenantRepository;
    private final PlatformTenantMapper platformTenantMapper;

    public CreatePlatformTenantUseCase(
            CreateTenantUseCase createTenantUseCase,
            TenantOwnerAdminProvisioningService tenantOwnerAdminProvisioningService
    ) {
        this(createTenantUseCase, tenantOwnerAdminProvisioningService, null, null);
    }

    @Autowired
    public CreatePlatformTenantUseCase(
            CreateTenantUseCase createTenantUseCase,
            TenantOwnerAdminProvisioningService tenantOwnerAdminProvisioningService,
            PlatformTenantRepository platformTenantRepository,
            PlatformTenantMapper platformTenantMapper
    ) {
        this.createTenantUseCase = createTenantUseCase;
        this.tenantOwnerAdminProvisioningService = tenantOwnerAdminProvisioningService;
        this.platformTenantRepository = platformTenantRepository;
        this.platformTenantMapper = platformTenantMapper;
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

        if (platformTenantRepository != null && tenantResponse.id() != null) {
            var existingTenantOpt = platformTenantRepository.findById(tenantResponse.id());
            if (existingTenantOpt.isPresent()) {
                var t = existingTenantOpt.get();
                String ownerFullName = (request.firstName() != null ? request.firstName().trim() : "") + " " +
                        (request.lastName() != null ? request.lastName().trim() : "");
                String ownerEmail = request.adminEmail() != null ? request.adminEmail().trim().toLowerCase() : null;

                var updated = new PlatformTenant(
                        t.id(),
                        t.name(),
                        t.slug(),
                        t.schemaName(),
                        t.status(),
                        t.planId(),
                        t.stripeCustomerId(),
                        t.active(),
                        t.legalName() != null ? t.legalName() : t.name(),
                        t.taxIdentifier(),
                        t.contactEmail() != null ? t.contactEmail() : ownerEmail,
                        t.contactPhone(),
                        ownerFullName.trim(),
                        ownerEmail,
                        t.createdAt(),
                        t.updatedAt()
                );
                var saved = platformTenantRepository.save(updated);
                if (platformTenantMapper != null) {
                    return platformTenantMapper.toResponse(saved);
                }
            }
        }

        return tenantResponse;
    }
}
