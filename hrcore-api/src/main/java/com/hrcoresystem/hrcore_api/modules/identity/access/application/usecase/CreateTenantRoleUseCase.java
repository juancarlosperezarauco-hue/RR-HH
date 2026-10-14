package com.hrcoresystem.hrcore_api.modules.identity.access.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.identity.audit.IdentityAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.CreateTenantRoleRequest;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.TenantRoleResponse;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.mapper.TenantRoleMapper;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.exception.InvalidSystemPermissionException;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.exception.TenantRoleAlreadyExistsException;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRole;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRoleSystemNames;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.service.PermissionAssignmentPolicy;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.policy.TenantOwnershipPolicy;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.validation.TenantRoleCodeValidator;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRolePermissionRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRoleRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CreateTenantRoleUseCase {

    private final TenantRoleRepository tenantRoleRepository;
    private final TenantRolePermissionRepository tenantRolePermissionRepository;
    private final PermissionAssignmentPolicy permissionAssignmentPolicy;
    private final TenantRoleMapper tenantRoleMapper;
    private final AuditTrailService auditTrailService;
    private final TenantPlanEnforcementService tenantPlanEnforcementService;
    private final TenantOwnershipPolicy tenantOwnershipPolicy;

    public CreateTenantRoleUseCase(
            TenantRoleRepository tenantRoleRepository,
            TenantRolePermissionRepository tenantRolePermissionRepository,
            PermissionAssignmentPolicy permissionAssignmentPolicy,
            TenantRoleMapper tenantRoleMapper,
            AuditTrailService auditTrailService,
            TenantPlanEnforcementService tenantPlanEnforcementService, TenantOwnershipPolicy tenantOwnershipPolicy
    ) {
        this.tenantRoleRepository = tenantRoleRepository;
        this.tenantRolePermissionRepository = tenantRolePermissionRepository;
        this.permissionAssignmentPolicy = permissionAssignmentPolicy;
        this.tenantRoleMapper = tenantRoleMapper;
        this.auditTrailService = auditTrailService;
        this.tenantPlanEnforcementService = tenantPlanEnforcementService;
        this.tenantOwnershipPolicy = tenantOwnershipPolicy;
    }

    @Transactional
    public TenantRoleResponse execute(CreateTenantRoleRequest request) {
        tenantOwnershipPolicy.requireCurrentOwner();
        String normalizedName = request.name().trim();
        String normalizedCode = TenantRoleCodeValidator.normalize(request.code());
        List<String> normalizedPermissionCodes = normalizePermissionCodes(request.permissionCodes());

        if (tenantRoleRepository.existsByName(normalizedName)) {
            throw new TenantRoleAlreadyExistsException(
                    "A tenant role with name '" + normalizedName + "' already exists"
            );
        }

        if (tenantRoleRepository.existsByCode(normalizedCode)) {
            throw new TenantRoleAlreadyExistsException(
                    "A tenant role with code '" + normalizedCode + "' already exists"
            );
        }

        permissionAssignmentPolicy.validate(normalizedPermissionCodes);

        tenantPlanEnforcementService.assertCanCreateRole(
                tenantRoleRepository.countActiveCustomRoles()
        );

        TenantRole roleToCreate = new TenantRole(
                null,
                normalizedCode,
                normalizedName,
                normalizeNullable(request.description()),
                false,
                true,
                null,
                null
        );

        TenantRole createdRole = tenantRoleRepository.save(roleToCreate);
        tenantRolePermissionRepository.replacePermissions(createdRole.id(), normalizedPermissionCodes);

        auditTrailService.recordTenantEvent(
                AuditEventTypes.ROLE_CREATED,
                "ROLE",
                createdRole.id().toString(),
                IdentityAuditPayloads.of(
                        "operation", "CREATE_ROLE",
                        "name", createdRole.name(),
                        "description", createdRole.description(),
                        "permissionCount", normalizedPermissionCodes.size()
                ),
                null,
                IdentityAuditPayloads.roleState(
                        createdRole.name(),
                        createdRole.description(),
                        createdRole.active(),
                        normalizedPermissionCodes.size()
                )
        );

        return tenantRoleMapper.toResponse(createdRole, normalizedPermissionCodes);
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
