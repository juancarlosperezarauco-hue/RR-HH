package com.hrcoresystem.hrcore_api.modules.identity.access.application.dto;

import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.PermissionScope;

public record SystemPermissionResponse(
        String code,
        String module,
        String description,
        PermissionScope scope,
        boolean customRoleAssignable,
        boolean includedInPlan
) {
    public SystemPermissionResponse(
            String code,
            String module,
            String description,
            PermissionScope scope,
            boolean customRoleAssignable
    ) {
        this(code, module, description, scope, customRoleAssignable, true);
    }
}
