package com.hrcoresystem.hrcore_api.modules.identity.access.application.mapper;

import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.TenantRoleResponse;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRole;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TenantRoleMapper {

    public TenantRoleResponse toResponse(TenantRole tenantRole, List<String> permissionCodes) {
        return new TenantRoleResponse(
                tenantRole.id(),
                tenantRole.code(),
                tenantRole.name(),
                tenantRole.description(),
                tenantRole.systemRole(),
                tenantRole.active(),
                tenantRole.createdAt(),
                tenantRole.updatedAt(),
                permissionCodes
        );
    }
}
