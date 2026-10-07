package com.hrcoresystem.hrcore_api.modules.identity.access.application.usecase;

import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.TenantRoleResponse;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.UserRolesResponse;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.mapper.TenantRoleMapper;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRolePermissionRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRoleRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantUserRoleRepository;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.exception.TenantUserNotFoundException;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GetUserRolesUseCase {

    private final TenantUserRepository tenantUserRepository;
    private final TenantRoleRepository tenantRoleRepository;
    private final TenantRolePermissionRepository tenantRolePermissionRepository;
    private final TenantUserRoleRepository tenantUserRoleRepository;
    private final TenantRoleMapper tenantRoleMapper;

    public GetUserRolesUseCase(
            TenantUserRepository tenantUserRepository,
            TenantRoleRepository tenantRoleRepository,
            TenantRolePermissionRepository tenantRolePermissionRepository,
            TenantUserRoleRepository tenantUserRoleRepository,
            TenantRoleMapper tenantRoleMapper
    ) {
        this.tenantUserRepository = tenantUserRepository;
        this.tenantRoleRepository = tenantRoleRepository;
        this.tenantRolePermissionRepository = tenantRolePermissionRepository;
        this.tenantUserRoleRepository = tenantUserRoleRepository;
        this.tenantRoleMapper = tenantRoleMapper;
    }

    public UserRolesResponse execute(UUID userId) {
        tenantUserRepository.findById(userId)
                .orElseThrow(() -> new TenantUserNotFoundException("Tenant user not found with id: " + userId));

        List<UUID> roleIds = tenantUserRoleRepository.findRoleIdsByUserId(userId);

        List<TenantRoleResponse> roles = tenantRoleRepository.findAllByIds(roleIds)
                .stream()
                .map(role -> tenantRoleMapper.toResponse(
                        role,
                        tenantRolePermissionRepository.findPermissionCodesByRoleId(role.id()).stream().toList()
                ))
                .toList();

        return new UserRolesResponse(userId, roles);
    }
}