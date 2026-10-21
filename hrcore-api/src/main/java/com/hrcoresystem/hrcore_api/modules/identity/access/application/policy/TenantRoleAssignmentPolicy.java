package com.hrcoresystem.hrcore_api.modules.identity.access.application.policy;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRole;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRoleType;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRoleRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TenantRoleAssignmentPolicy {

    private final TenantRoleRepository repo;
    private final TenantOwnershipPolicy ownership;

    public TenantRoleAssignmentPolicy(TenantRoleRepository repo, TenantOwnershipPolicy ownership) {
        this.repo = repo;
        this.ownership = ownership;
    }

    public List<TenantRole> validateAssignableRoleIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        Set<UUID> seen = new HashSet<>();
        for (UUID id : ids) {
            if (id == null || !seen.add(id)) {
                throw new IllegalArgumentException("Role ids must be unique and non-null");
            }
        }

        List<TenantRole> found = repo.findAllByIds(ids);
        if (found.size() != ids.size()) {
            throw new BusinessException("Uno o más roles no existen en la organización");
        }

        Map<UUID, TenantRole> by = found.stream().collect(Collectors.toMap(TenantRole::id, r -> r));
        List<TenantRole> result = new ArrayList<>();
        for (UUID id : ids) {
            TenantRole role = by.get(id);
            if (role == null) {
                throw new BusinessException("El rol no pertenece a la organización");
            }
            if (!role.active()) {
                throw new BusinessException("No es posible asignar el rol '" + role.name() + "' porque se encuentra inactivo");
            }
            if (role.isSystemRole() || role.roleType() != TenantRoleType.INTERNAL) {
                throw new AccessDeniedException("No se pueden asignar roles reservados del sistema");
            }
            ownership.assertRoleAssignable(role);
            result.add(role);
        }
        return result;
    }
}
