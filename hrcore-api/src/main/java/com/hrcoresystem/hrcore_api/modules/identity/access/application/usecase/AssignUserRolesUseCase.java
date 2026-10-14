package com.hrcoresystem.hrcore_api.modules.identity.access.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.identity.audit.IdentityAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.AssignUserRolesRequest;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.TenantRoleResponse;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.UserRolesResponse;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.mapper.TenantRoleMapper;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.service.TenantRoleAssignmentService;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.policy.TenantOwnershipPolicy;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.exception.TenantRoleNotFoundException;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRole;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRolePermissionRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRoleRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantUserRoleRepository;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.exception.TenantUserNotFoundException;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AssignUserRolesUseCase {

    private final TenantUserRepository tenantUserRepository;
    private final TenantRoleRepository tenantRoleRepository;
    private final TenantRolePermissionRepository tenantRolePermissionRepository;
    private final TenantUserRoleRepository tenantUserRoleRepository;
    private final TenantRoleMapper tenantRoleMapper;
    private final AuditTrailService auditTrailService;
    private final TenantRoleAssignmentService assignmentService;
    private final TenantOwnershipPolicy ownershipPolicy;

    public AssignUserRolesUseCase(
            TenantUserRepository tenantUserRepository,
            TenantRoleRepository tenantRoleRepository,
            TenantRolePermissionRepository tenantRolePermissionRepository,
            TenantUserRoleRepository tenantUserRoleRepository,
            TenantRoleMapper tenantRoleMapper,
            AuditTrailService auditTrailService, TenantRoleAssignmentService assignmentService, TenantOwnershipPolicy ownershipPolicy
    ) {
        this.tenantUserRepository = tenantUserRepository;
        this.tenantRoleRepository = tenantRoleRepository;
        this.tenantRolePermissionRepository = tenantRolePermissionRepository;
        this.tenantUserRoleRepository = tenantUserRoleRepository;
        this.tenantRoleMapper = tenantRoleMapper;
        this.auditTrailService = auditTrailService;
        this.assignmentService = assignmentService;
        this.ownershipPolicy = ownershipPolicy;
    }

    @Transactional
    public UserRolesResponse execute(UUID userId, AssignUserRolesRequest request) {
        ownershipPolicy.requireCurrentOwner();
        tenantUserRepository.findById(userId)
                .orElseThrow(() -> new TenantUserNotFoundException("Tenant user not found with id: " + userId));

        List<UUID> roleIds = request.roleIds() == null ? List.of() : request.roleIds();

        ownershipPolicy.assertNotCurrentOwner(userId);
        List<String> previousRoleCodes = tenantUserRoleRepository.findRoleCodesByUserId(userId);
        List<TenantRole> roles = assignmentService.replaceRoles(userId, roleIds);

        List<TenantRoleResponse> roleResponses = roles.stream()
                .map(role -> tenantRoleMapper.toResponse(
                        role,
                        tenantRolePermissionRepository.findPermissionCodesByRoleId(role.id()).stream().toList()
                ))
                .toList();

        auditTrailService.recordTenantEvent(
                AuditEventTypes.USER_ROLES_ASSIGNED,
                "USER",
                userId.toString(),
                IdentityAuditPayloads.of(
                        "operation", "ASSIGN_USER_ROLES",
                        "roleIds", roleIds,
                        "roleCount", roleIds.size()
                ),
                IdentityAuditPayloads.of(
                        "roleCodes", previousRoleCodes,
                        "roleCount", previousRoleCodes.size()
                ),
                IdentityAuditPayloads.of(
                        "roleCodes", roleResponses.stream().map(TenantRoleResponse::code).toList(),
                        "roleCount", roleResponses.size()
                )
        );

        return new UserRolesResponse(userId, roleResponses);
    }
}
