package com.hrcoresystem.hrcore_api.modules.identity.access.application.policy;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.common.security.authorization.domain.repository.TenantAuthorizationStateRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRole;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRoleRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantUserRoleRepository;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import java.util.UUID;
@Service public class TenantOwnershipPolicy {
    private final SecurityContextFacade security; private final TenantAuthorizationStateRepository state; private final TenantUserRepository users; private final TenantRoleRepository roles; private final TenantUserRoleRepository userRoles;
    public TenantOwnershipPolicy(SecurityContextFacade security,TenantAuthorizationStateRepository state,TenantUserRepository users,TenantRoleRepository roles,TenantUserRoleRepository userRoles){this.security=security;this.state=state;this.users=users;this.roles=roles;this.userRoles=userRoles;}
    public boolean isCurrentOwner(){try{UUID id=UUID.fromString(security.getCurrentSubject()); var owner=state.ownerUserId(); var u=users.findById(id); return owner!=null&&owner.equals(id)&&u.isPresent()&&u.get().active()&&u.get().status().name().equals("ACTIVE")&&userRoles.findRoleIdsByUserId(id).stream().map(roles::findById).flatMap(java.util.Optional::stream).anyMatch(r->r.systemRole()&&"OWNER_ADMIN".equals(r.code())&&r.active());}catch(Exception e){return false;}}
    public void requireCurrentOwner(){if(!isCurrentOwner())throw new AccessDeniedException("Current tenant owner required");}
    public void assertNotCurrentOwner(UUID id){if(id!=null&&id.equals(state.ownerUserId()))throw new AccessDeniedException("Current tenant owner cannot be modified");}
    public void assertRoleIsCustom(TenantRole role){if(role==null||role.systemRole()||"OWNER_ADMIN".equals(role.code()))throw new AccessDeniedException("System roles cannot be modified");}
    public void assertRoleAssignable(TenantRole role){if(role==null||role.systemRole()||!role.active()||"OWNER_ADMIN".equals(role.code()))throw new AccessDeniedException("Role cannot be assigned");}
}
