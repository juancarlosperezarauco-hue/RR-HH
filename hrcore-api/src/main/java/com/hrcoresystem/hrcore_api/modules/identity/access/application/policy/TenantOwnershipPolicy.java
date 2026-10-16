package com.hrcoresystem.hrcore_api.modules.identity.access.application.policy;

import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.common.security.authorization.domain.repository.TenantAuthorizationStateRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRole;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRoleType;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRoleRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantUserRoleRepository;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class TenantOwnershipPolicy {
    private final SecurityContextFacade security;
    private final TenantAuthorizationStateRepository state;
    private final TenantUserRepository users;
    private final TenantRoleRepository roles;
    private final TenantUserRoleRepository userRoles;

    public TenantOwnershipPolicy(SecurityContextFacade security,
            TenantAuthorizationStateRepository state,
            TenantUserRepository users,
            TenantRoleRepository roles,
            TenantUserRoleRepository userRoles) {
        this.security = security;
        this.state = state;
        this.users = users;
        this.roles = roles;
        this.userRoles = userRoles;
    }

    public boolean isCurrentOwner() {
        try {
            UUID userId = UUID.fromString(security.getCurrentSubject());
            UUID ownerId = state.ownerUserId();
            var user = users.findById(userId);
            return ownerId != null
                    && ownerId.equals(userId)
                    && user.isPresent()
                    && user.get().active()
                    && user.get().status().name().equals("ACTIVE")
                    && userRoles.findRoleIdsByUserId(userId).stream()
                    .map(roles::findById)
                    .flatMap(java.util.Optional::stream)
                    .anyMatch(role -> role.isOwnerAdmin() && role.active());
        } catch (IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }

    public void requireCurrentOwner() {
        if (!isCurrentOwner()) throw new AccessDeniedException("Current tenant owner required");
    }

    public void assertNotCurrentOwner(UUID id) {
        if (id != null && id.equals(state.ownerUserId()))
            throw new AccessDeniedException("Current tenant owner cannot be modified");
    }

    public void assertRoleIsCustom(TenantRole role) {
        if (role == null || role.roleType() != TenantRoleType.INTERNAL || role.systemRole())
            throw new AccessDeniedException("System roles cannot be modified");
    }

    public void assertRoleAssignable(TenantRole role) {
        if (role == null || role.roleType() != TenantRoleType.INTERNAL || role.systemRole() || !role.active())
            throw new AccessDeniedException("Role cannot be assigned");
    }
}
