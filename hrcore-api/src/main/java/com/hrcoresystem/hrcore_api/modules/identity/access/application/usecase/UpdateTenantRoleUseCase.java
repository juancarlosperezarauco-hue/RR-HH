package com.hrcoresystem.hrcore_api.modules.identity.access.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.identity.audit.IdentityAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.TenantRoleResponse;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.UpdateTenantRoleRequest;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.mapper.TenantRoleMapper;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.exception.InvalidSystemPermissionException;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.exception.TenantRoleAlreadyExistsException;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.exception.TenantRoleNotFoundException;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRole;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.service.PermissionAssignmentPolicy;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.policy.TenantOwnershipPolicy;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRolePermissionRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class UpdateTenantRoleUseCase {

    private final TenantRoleRepository tenantRoleRepository;
    private final TenantRolePermissionRepository tenantRolePermissionRepository;
    private final PermissionAssignmentPolicy permissionAssignmentPolicy;
    private final TenantRoleMapper tenantRoleMapper;
    private final AuditTrailService auditTrailService;
    private final TenantOwnershipPolicy tenantOwnershipPolicy;

    public UpdateTenantRoleUseCase(
            TenantRoleRepository tenantRoleRepository,
            TenantRolePermissionRepository tenantRolePermissionRepository,
            PermissionAssignmentPolicy permissionAssignmentPolicy,
            TenantRoleMapper tenantRoleMapper,
            AuditTrailService auditTrailService, TenantOwnershipPolicy tenantOwnershipPolicy
    ) {
        this.tenantRoleRepository = tenantRoleRepository;
        this.tenantRolePermissionRepository = tenantRolePermissionRepository;
        this.permissionAssignmentPolicy = permissionAssignmentPolicy;
        this.tenantRoleMapper = tenantRoleMapper;
        this.auditTrailService = auditTrailService;
        this.tenantOwnershipPolicy = tenantOwnershipPolicy;
    }

    @Transactional
    public TenantRoleResponse execute(UUID id, UpdateTenantRoleRequest request) {
        tenantOwnershipPolicy.requireCurrentOwner();
        TenantRole existingRole = tenantRoleRepository.findById(id)
                .orElseThrow(() -> new TenantRoleNotFoundException("Tenant role not found with id: " + id));

        tenantOwnershipPolicy.assertRoleIsCustom(existingRole);

        String normalizedName = request.name().trim().toUpperCase();
        List<String> normalizedPermissionCodes = normalizePermissionCodes(request.permissionCodes());

        tenantRoleRepository.findByName(normalizedName)
                .filter(foundRole -> !foundRole.id().equals(id))
                .ifPresent(foundRole -> {
                    throw new TenantRoleAlreadyExistsException(
                            "A tenant role with name '" + normalizedName + "' already exists"
                    );
                });

        permissionAssignmentPolicy.validate(normalizedPermissionCodes);

        List<String> existingPermissionCodes = tenantRolePermissionRepository
                .findPermissionCodesByRoleId(existingRole.id())
                .stream()
                .toList();

        TenantRole updatedRole = new TenantRole(
                existingRole.id(),
                existingRole.code(),
                normalizedName,
                normalizeNullable(request.description()),
                existingRole.systemRole(),
                existingRole.active(),
                existingRole.createdAt(),
                existingRole.updatedAt()
        );

        TenantRole savedRole = tenantRoleRepository.save(updatedRole);
        tenantRolePermissionRepository.replacePermissions(savedRole.id(), normalizedPermissionCodes);

        auditTrailService.recordTenantEvent(
                AuditEventTypes.ROLE_UPDATED,
                "ROLE",
                savedRole.id().toString(),
                IdentityAuditPayloads.of(
                        "operation", "UPDATE_ROLE",
                        "previousName", existingRole.name(),
                        "newName", savedRole.name(),
                        "description", savedRole.description(),
                        "previousPermissionCount", existingPermissionCodes.size(),
                        "newPermissionCount", normalizedPermissionCodes.size()
                ),
                IdentityAuditPayloads.roleState(
                        existingRole.name(),
                        existingRole.description(),
                        existingRole.active(),
                        existingPermissionCodes.size()
                ),
                IdentityAuditPayloads.roleState(
                        savedRole.name(),
                        savedRole.description(),
                        savedRole.active(),
                        normalizedPermissionCodes.size()
                )
        );

        return tenantRoleMapper.toResponse(savedRole, normalizedPermissionCodes);
    }

    private List<String> normalizePermissionCodes(List<String> permissionCodes) {
        if (permissionCodes == null) {
            return List.of();
        }

        return permissionCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
