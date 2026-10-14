package com.hrcoresystem.hrcore_api.modules.identity.access.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.identity.audit.IdentityAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.TenantRoleResponse;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.mapper.TenantRoleMapper;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.policy.TenantOwnershipPolicy;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.exception.TenantRoleNotFoundException;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRole;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRolePermissionRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRoleRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class ActivateTenantRoleUseCase {

    private final TenantRoleRepository tenantRoleRepository;
    private final TenantRolePermissionRepository tenantRolePermissionRepository;
    private final TenantRoleMapper tenantRoleMapper;
    private final AuditTrailService auditTrailService;
    private final TenantPlanEnforcementService tenantPlanEnforcementService;
    private final TenantOwnershipPolicy tenantOwnershipPolicy;

    public ActivateTenantRoleUseCase(
            TenantRoleRepository tenantRoleRepository,
            TenantRolePermissionRepository tenantRolePermissionRepository,
            TenantRoleMapper tenantRoleMapper,
            AuditTrailService auditTrailService,
            TenantPlanEnforcementService tenantPlanEnforcementService, TenantOwnershipPolicy tenantOwnershipPolicy
    ) {
        this.tenantRoleRepository = tenantRoleRepository;
        this.tenantRolePermissionRepository = tenantRolePermissionRepository;
        this.tenantRoleMapper = tenantRoleMapper;
        this.auditTrailService = auditTrailService;
        this.tenantPlanEnforcementService = tenantPlanEnforcementService;
        this.tenantOwnershipPolicy = tenantOwnershipPolicy;
    }

    @Transactional
    public TenantRoleResponse execute(UUID id) {
        tenantOwnershipPolicy.requireCurrentOwner();
        TenantRole existingRole = tenantRoleRepository.findById(id)
                .orElseThrow(() -> new TenantRoleNotFoundException("Tenant role not found with id: " + id));

        tenantOwnershipPolicy.assertRoleIsCustom(existingRole);

        if (!existingRole.active()) {
            tenantPlanEnforcementService.assertCanActivateRole(
                    tenantRoleRepository.countActiveCustomRoles()
            );
        }

        java.util.List<String> existingPermissionCodes = tenantRolePermissionRepository
                .findPermissionCodesByRoleId(existingRole.id())
                .stream()
                .toList();

        TenantRole updatedRole = new TenantRole(
                existingRole.id(),
                existingRole.code(),
                existingRole.name(),
                existingRole.description(),
                existingRole.systemRole(),
                true,
                existingRole.createdAt(),
                existingRole.updatedAt()
        );

        TenantRole savedRole = tenantRoleRepository.save(updatedRole);

        auditTrailService.recordTenantEvent(
                AuditEventTypes.ROLE_ACTIVATED,
                "ROLE",
                savedRole.id().toString(),
                IdentityAuditPayloads.of(
                        "operation", "ACTIVATE_ROLE",
                        "name", savedRole.name(),
                        "active", true
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
                        existingPermissionCodes.size()
                )
        );

        return tenantRoleMapper.toResponse(
                savedRole,
                tenantRolePermissionRepository.findPermissionCodesByRoleId(savedRole.id()).stream().toList()
        );
    }
}
