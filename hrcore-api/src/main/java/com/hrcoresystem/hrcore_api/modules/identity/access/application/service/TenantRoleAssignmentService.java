package com.hrcoresystem.hrcore_api.modules.identity.access.application.service;

import com.hrcoresystem.hrcore_api.modules.identity.access.application.policy.TenantRoleAssignmentPolicy;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.policy.TenantOwnershipPolicy;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRole;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantUserRoleRepository;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class TenantRoleAssignmentService {
    private final TenantRoleAssignmentPolicy policy; private final TenantUserRoleRepository assignments; private final TenantUserRepository users; private final TenantOwnershipPolicy ownership;
    public TenantRoleAssignmentService(TenantRoleAssignmentPolicy policy,TenantUserRoleRepository assignments,TenantUserRepository users,TenantOwnershipPolicy ownership){this.policy=policy;this.assignments=assignments;this.users=users;this.ownership=ownership;}
    @Transactional public List<TenantRole> assignInitialRoles(UUID userId,List<UUID> ids){List<TenantRole> roles=policy.validateAssignableRoleIds(ids);if(!roles.isEmpty())assignments.replaceUserRoles(userId,ids);return roles;}
    @Transactional public List<TenantRole> replaceRoles(UUID userId,List<UUID> ids){users.findById(userId).orElseThrow();ownership.assertNotCurrentOwner(userId);List<TenantRole> roles=policy.validateAssignableRoleIds(ids);if(assignments.replaceUserRoles(userId,ids))users.incrementSecurityVersion(userId);return roles;}
}
