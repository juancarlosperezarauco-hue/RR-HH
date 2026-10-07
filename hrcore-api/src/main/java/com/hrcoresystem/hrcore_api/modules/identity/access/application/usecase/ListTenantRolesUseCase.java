package com.hrcoresystem.hrcore_api.modules.identity.access.application.usecase;

import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.TenantRoleResponse;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.mapper.TenantRoleMapper;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRolePermissionRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListTenantRolesUseCase {

    private final TenantRoleRepository tenantRoleRepository;
    private final TenantRolePermissionRepository tenantRolePermissionRepository;
    private final TenantRoleMapper tenantRoleMapper;

    public ListTenantRolesUseCase(
            TenantRoleRepository tenantRoleRepository,
            TenantRolePermissionRepository tenantRolePermissionRepository,
            TenantRoleMapper tenantRoleMapper
    ) {
        this.tenantRoleRepository = tenantRoleRepository;
        this.tenantRolePermissionRepository = tenantRolePermissionRepository;
        this.tenantRoleMapper = tenantRoleMapper;
    }

    public List<TenantRoleResponse> execute() {
        return tenantRoleRepository.findAll()
                .stream()
                .map(role -> tenantRoleMapper.toResponse(
                        role,
                        tenantRolePermissionRepository.findPermissionCodesByRoleId(role.id())
                                .stream()
                                .toList()
                ))
                .toList();
    }
}