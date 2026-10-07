package com.hrcoresystem.hrcore_api.modules.identity.access.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.identity.audit.IdentityAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.TenantRoleResponse;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.mapper.TenantRoleMapper;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.exception.TenantRoleNotFoundException;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRole;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRolePermissionRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class DeactivateTenantRoleUseCase {

    private final TenantRoleRepository tenantRoleRepository;
    private final TenantRolePermissionRepository tenantRolePermissionRepository;
    private final TenantRoleMapper tenantRoleMapper;
    private final AuditTrailService auditTrailService;

    public DeactivateTenantRoleUseCase(
            TenantRoleRepository tenantRoleRepository,
            TenantRolePermissionRepository tenantRolePermissionRepository,
            TenantRoleMapper tenantRoleMapper,
            AuditTrailService auditTrailService
    ) {
        this.tenantRoleRepository = tenantRoleRepository;
        this.tenantRolePermissionRepository = tenantRolePermissionRepository;
        this.tenantRoleMapper = tenantRoleMapper;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public TenantRoleResponse execute(UUID id) {
        TenantRole existingRole = tenantRoleRepository.findById(id)
                .orElseThrow(() -> new TenantRoleNotFoundException("Tenant role not found with id: " + id));

        java.util.List<String> existingPermissionCodes = tenantRolePermissionRepository
                .findPermissionCodesByRoleId(existingRole.id())
                .stream()
                .toList();

        TenantRole updatedRole = new TenantRole(
                existingRole.id(),
                existingRole.name(),
                existingRole.description(),
                false,
                existingRole.createdAt()
        );

        TenantRole savedRole = tenantRoleRepository.save(updatedRole);

        auditTrailService.recordTenantEvent(
                AuditEventTypes.ROLE_DEACTIVATED,
                "ROLE",
                savedRole.id().toString(),
                IdentityAuditPayloads.of(
                        "operation", "DEACTIVATE_ROLE",
                        "name", savedRole.name(),
                        "active", false
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
