package com.hrcoresystem.hrcore_api.modules.identity.access.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.identity.audit.IdentityAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.TenantRoleResponse;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.UpdateTenantRoleRequest;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.mapper.TenantRoleMapper;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.exception.TenantRoleAlreadyExistsException;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.exception.TenantRoleNotFoundException;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.SystemPermission;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRole;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.service.PermissionAssignmentPolicy;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.service.TenantAuthorizationRevisionService;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.policy.TenantOwnershipPolicy;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.SystemPermissionRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRolePermissionRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRoleRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UpdateTenantRoleUseCase {

    private final TenantRoleRepository tenantRoleRepository;
    private final TenantRolePermissionRepository tenantRolePermissionRepository;
    private final PermissionAssignmentPolicy permissionAssignmentPolicy;
    private final TenantRoleMapper tenantRoleMapper;
    private final AuditTrailService auditTrailService;
    private final TenantOwnershipPolicy tenantOwnershipPolicy;
    private final TenantAuthorizationRevisionService revisionService;
    private final TenantPlanEnforcementService tenantPlanEnforcementService;
    private final SystemPermissionRepository systemPermissionRepository;

    @Autowired
    public UpdateTenantRoleUseCase(
            TenantRoleRepository tenantRoleRepository,
            TenantRolePermissionRepository tenantRolePermissionRepository,
            PermissionAssignmentPolicy permissionAssignmentPolicy,
            TenantRoleMapper tenantRoleMapper,
            AuditTrailService auditTrailService,
            TenantOwnershipPolicy tenantOwnershipPolicy,
            TenantAuthorizationRevisionService revisionService,
            @Autowired(required = false) TenantPlanEnforcementService tenantPlanEnforcementService,
            @Autowired(required = false) SystemPermissionRepository systemPermissionRepository
    ) {
        this.tenantRoleRepository = tenantRoleRepository;
        this.tenantRolePermissionRepository = tenantRolePermissionRepository;
        this.permissionAssignmentPolicy = permissionAssignmentPolicy;
        this.tenantRoleMapper = tenantRoleMapper;
        this.auditTrailService = auditTrailService;
        this.tenantOwnershipPolicy = tenantOwnershipPolicy;
        this.revisionService = revisionService;
        this.tenantPlanEnforcementService = tenantPlanEnforcementService;
        this.systemPermissionRepository = systemPermissionRepository;
    }

    public UpdateTenantRoleUseCase(
            TenantRoleRepository tenantRoleRepository,
            TenantRolePermissionRepository tenantRolePermissionRepository,
            PermissionAssignmentPolicy permissionAssignmentPolicy,
            TenantRoleMapper tenantRoleMapper,
            AuditTrailService auditTrailService,
            TenantOwnershipPolicy tenantOwnershipPolicy,
            TenantAuthorizationRevisionService revisionService
    ) {
        this(tenantRoleRepository, tenantRolePermissionRepository, permissionAssignmentPolicy,
                tenantRoleMapper, auditTrailService, tenantOwnershipPolicy, revisionService, null, null);
    }

    @Transactional
    public TenantRoleResponse execute(UUID id, UpdateTenantRoleRequest request) {
        tenantOwnershipPolicy.requireCurrentOwner();
        TenantRole existingRole = tenantRoleRepository.findById(id)
                .orElseThrow(() -> new TenantRoleNotFoundException("Tenant role not found with id: " + id));

        tenantOwnershipPolicy.assertRoleIsCustom(existingRole);

        String normalizedName = request.name().trim();
        List<String> normalizedPermissionCodes = normalizePermissionCodes(request.permissionCodes());

        tenantRoleRepository.findByName(normalizedName)
                .filter(foundRole -> !foundRole.id().equals(id))
                .ifPresent(foundRole -> {
                    throw new TenantRoleAlreadyExistsException(
                            "Ya existe un rol con el nombre '" + normalizedName + "' en esta organización. Por favor elija un nombre diferente."
                    );
                });

        permissionAssignmentPolicy.validate(normalizedPermissionCodes);

        if (tenantPlanEnforcementService != null) {
            Map<String, String> codeToModule = systemPermissionRepository != null
                    ? systemPermissionRepository.findAllActiveTenant().stream()
                            .collect(Collectors.toMap(SystemPermission::code, SystemPermission::module, (a, b) -> a))
                    : Map.of();

            tenantPlanEnforcementService.assertCanAssignPermissions(normalizedPermissionCodes, codeToModule);
        }

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
                existingRole.updatedAt(),
                existingRole.roleType()
        );

        TenantRole savedRole = tenantRoleRepository.save(updatedRole);
        tenantRolePermissionRepository.replacePermissions(savedRole.id(), normalizedPermissionCodes);
        if (!new java.util.HashSet<>(existingPermissionCodes).equals(new java.util.HashSet<>(normalizedPermissionCodes))) {
            revisionService.incrementAuthorizationVersion();
        }

        auditTrailService.recordTenantEvent(
                AuditEventTypes.ROLE_UPDATED,
                "ROLE",
                savedRole.id().toString(),
                IdentityAuditPayloads.of(
                        "operation", "UPDATE_ROLE",
                        "code", savedRole.code(),
                        "name", savedRole.name(),
                        "description", savedRole.description(),
                        "permissionCount", normalizedPermissionCodes.size()
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
        if (permissionCodes == null) return List.of();
        return permissionCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private String normalizeNullable(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
