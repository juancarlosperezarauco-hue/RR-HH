package com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface TenantRolePermissionRepository {

    Set<String> findPermissionCodesByRoleId(UUID roleId);

    void replacePermissions(UUID roleId, List<String> permissionCodes);
}