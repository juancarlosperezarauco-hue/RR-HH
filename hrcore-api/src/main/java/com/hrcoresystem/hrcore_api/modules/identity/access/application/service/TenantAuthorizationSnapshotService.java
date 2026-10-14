package com.hrcoresystem.hrcore_api.modules.identity.access.application.service;

import com.hrcoresystem.hrcore_api.common.security.authorization.domain.repository.AuthorizationCatalogStateRepository;
import com.hrcoresystem.hrcore_api.common.security.authorization.domain.repository.TenantAuthorizationStateRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantUserRoleRepository;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class TenantAuthorizationSnapshotService {
    private final TenantUserRepository users;
    private final TenantUserRoleRepository userRoles;
    private final TenantAuthorizationStateRepository authorizationState;
    private final AuthorizationCatalogStateRepository catalogState;

    public TenantAuthorizationSnapshotService(TenantUserRepository users, TenantUserRoleRepository userRoles,
            TenantAuthorizationStateRepository authorizationState, AuthorizationCatalogStateRepository catalogState) {
        this.users = users; this.userRoles = userRoles; this.authorizationState = authorizationState; this.catalogState = catalogState;
    }

    public TenantAuthorizationSnapshot getSnapshot(UUID userId) {
        var user = users.findById(userId).orElseThrow(() -> new IllegalStateException("Tenant user not found"));
        var state = authorizationState.find().orElseThrow(() -> new IllegalStateException("Tenant authorization state is missing"));
        var catalog = catalogState.find().orElseThrow(() -> new IllegalStateException("Authorization catalog state is missing"));
        List<String> roles = List.copyOf(userRoles.findRoleCodesByUserId(userId));
        List<String> permissions = List.copyOf(userRoles.findPermissionCodesByUserId(userId));
        return new TenantAuthorizationSnapshot(userId, roles, permissions, user.securityVersion(), state.authorizationVersion(), catalog.catalogVersion());
    }

    public record TenantAuthorizationSnapshot(UUID userId, List<String> roleCodes, List<String> permissionCodes,
            long securityVersion, long authorizationVersion, long catalogVersion) {}
}
