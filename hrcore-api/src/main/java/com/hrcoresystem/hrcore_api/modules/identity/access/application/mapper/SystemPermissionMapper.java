package com.hrcoresystem.hrcore_api.modules.identity.access.application.mapper;

import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.SystemPermissionResponse;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.SystemPermission;
import org.springframework.stereotype.Component;

@Component
public class SystemPermissionMapper {

    public SystemPermissionResponse toResponse(SystemPermission systemPermission) {
        return toResponse(systemPermission, true);
    }

    public SystemPermissionResponse toResponse(SystemPermission systemPermission, boolean includedInPlan) {
        return new SystemPermissionResponse(
                systemPermission.code(),
                systemPermission.module(),
                systemPermission.description(),
                systemPermission.scope(),
                systemPermission.customRoleAssignable(),
                includedInPlan
        );
    }
}
