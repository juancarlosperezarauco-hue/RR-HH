package com.hrcoresystem.hrcore_api.modules.identity.access.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.security.authorization.domain.repository.TenantAuthorizationStateRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.policy.TenantOwnershipPolicy;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.policy.TenantRoleAssignmentPolicy;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRole;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRoleType;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRolePermissionRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRoleRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantUserRoleRepository;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUserStatus;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TenantRoleAssignmentService {

    private final TenantRoleAssignmentPolicy policy;
    private final TenantUserRoleRepository assignments;
    private final TenantUserRepository users;
    private final TenantOwnershipPolicy ownership;
    private final TenantAuthorizationStateRepository authorizationState;
    private final TenantRoleRepository roleRepository;
    private final TenantRolePermissionRepository rolePermissionRepository;

    public TenantRoleAssignmentService(
            TenantRoleAssignmentPolicy policy,
            TenantUserRoleRepository assignments,
            TenantUserRepository users,
            TenantOwnershipPolicy ownership,
            TenantAuthorizationStateRepository authorizationState,
            TenantRoleRepository roleRepository,
            TenantRolePermissionRepository rolePermissionRepository
    ) {
        this.policy = policy;
        this.assignments = assignments;
        this.users = users;
        this.ownership = ownership;
        this.authorizationState = authorizationState;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Transactional
    public List<TenantRole> assignInitialRoles(UUID userId, List<UUID> ids) {
        List<TenantRole> roles = policy.validateAssignableRoleIds(ids);
        if (!roles.isEmpty()) {
            assignments.replaceUserRoles(userId, ids);
        }
        return roles;
    }

    @Transactional
    public List<TenantRole> replaceRoles(UUID userId, List<UUID> ids) {
        users.findById(userId).orElseThrow();

        // 1. Proteger al único administrador activo de la organización (Criterio 5)
        assertAdministrativeRoleRetention(userId, ids);

        // 2. Proteger al propietario
        ownership.assertNotCurrentOwner(userId);

        // 3. Validar roles asignables (activos, válidos y pertenecientes al tenant) (Criterio 4)
        List<TenantRole> roles = policy.validateAssignableRoleIds(ids);

        // 4. Actualizar asignaciones y actualizar versión de seguridad para reflejar de inmediato los permisos (Criterios 2, 3, 6)
        if (assignments.replaceUserRoles(userId, ids)) {
            users.incrementSecurityVersion(userId);
        }
        return roles;
    }

    public void assertAdministrativeRoleRetention(UUID userId, List<UUID> newRoleIds) {
        UUID ownerId = authorizationState.ownerUserId();
        if (userId != null && userId.equals(ownerId)) {
            throw new BusinessException("No es posible retirar el rol administrativo cuando el usuario es el único administrador activo de la organización");
        }

        List<UUID> currentRoleIds = assignments.findRoleIdsByUserId(userId);
        boolean currentlyAdmin = isAnyRoleAdministrative(currentRoleIds);

        if (!currentlyAdmin) {
            return;
        }

        boolean willRemainAdmin = isAnyRoleAdministrative(newRoleIds);
        if (willRemainAdmin) {
            return;
        }

        long otherActiveAdmins = countOtherActiveAdministrators(userId);
        if (otherActiveAdmins == 0) {
            throw new BusinessException("No es posible retirar el rol administrativo cuando el usuario es el único administrador activo de la organización");
        }
    }

    private boolean isAnyRoleAdministrative(List<UUID> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return false;
        }
        List<TenantRole> roles = roleRepository.findAllByIds(roleIds);
        for (TenantRole role : roles) {
            if (role.isOwnerAdmin() || role.roleType() == TenantRoleType.ROOT) {
                return true;
            }
            if ("OWNER_ADMIN".equalsIgnoreCase(role.code()) ||
                "ADMIN".equalsIgnoreCase(role.code()) ||
                "Administrador".equalsIgnoreCase(role.name())) {
                return true;
            }
            Set<String> perms = rolePermissionRepository.findPermissionCodesByRoleId(role.id());
            if (perms.contains("access.users.roles.assign") ||
                perms.contains("access.roles.create") ||
                perms.contains("access.roles.update")) {
                return true;
            }
        }
        return false;
    }

    private long countOtherActiveAdministrators(UUID currentUserId) {
        long count = 0;
        UUID ownerId = authorizationState.ownerUserId();
        if (ownerId != null && !ownerId.equals(currentUserId)) {
            var ownerUser = users.findById(ownerId);
            if (ownerUser.isPresent() && ownerUser.get().active() && ownerUser.get().status() == TenantUserStatus.ACTIVE) {
                count++;
            }
        }

        List<TenantUser> allUsers = users.findAll();
        for (TenantUser user : allUsers) {
            if (user.id().equals(currentUserId) || (ownerId != null && user.id().equals(ownerId))) {
                continue;
            }
            if (user.active() && user.status() == TenantUserStatus.ACTIVE) {
                List<UUID> userRoleIds = assignments.findRoleIdsByUserId(user.id());
                if (isAnyRoleAdministrative(userRoleIds)) {
                    count++;
                }
            }
        }
        return count;
    }
}
