package com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository;

import java.util.List;
import java.util.UUID;

public interface TenantUserRoleRepository {

    List<UUID> findRoleIdsByUserId(UUID userId);

    List<String> findRoleNamesByUserId(UUID userId);

    List<String> findPermissionCodesByUserId(UUID userId);

    void replaceUserRoles(UUID userId, List<UUID> roleIds);
}
