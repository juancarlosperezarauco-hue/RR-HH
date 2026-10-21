package com.hrcoresystem.hrcore_api.modules.identity.access.application.usecase;

import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.SystemPermissionResponse;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.mapper.SystemPermissionMapper;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.SystemPermissionRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListSystemPermissionsUseCase {

    private final SystemPermissionRepository systemPermissionRepository;
    private final SystemPermissionMapper systemPermissionMapper;
    private final TenantPlanEnforcementService tenantPlanEnforcementService;

    @Autowired
    public ListSystemPermissionsUseCase(
            SystemPermissionRepository systemPermissionRepository,
            SystemPermissionMapper systemPermissionMapper,
            @Autowired(required = false) TenantPlanEnforcementService tenantPlanEnforcementService
    ) {
        this.systemPermissionRepository = systemPermissionRepository;
        this.systemPermissionMapper = systemPermissionMapper;
        this.tenantPlanEnforcementService = tenantPlanEnforcementService;
    }

    public List<SystemPermissionResponse> execute() {
        return systemPermissionRepository.findAllActiveTenant()
                .stream()
                .map(permission -> {
                    boolean included = tenantPlanEnforcementService == null
                            || tenantPlanEnforcementService.isModuleIncludedInCurrentPlan(permission.module());
                    return systemPermissionMapper.toResponse(permission, included);
                })
                .toList();
    }
}
