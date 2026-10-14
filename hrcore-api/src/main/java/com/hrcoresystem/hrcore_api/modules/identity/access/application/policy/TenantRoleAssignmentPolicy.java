package com.hrcoresystem.hrcore_api.modules.identity.access.application.policy;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRoleRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRole;
import org.springframework.stereotype.Service;
import java.util.*;
@Service public class TenantRoleAssignmentPolicy {
 private final TenantRoleRepository repo; private final TenantOwnershipPolicy ownership;
 public TenantRoleAssignmentPolicy(TenantRoleRepository repo,TenantOwnershipPolicy ownership){this.repo=repo;this.ownership=ownership;}
 public List<TenantRole> validateAssignableRoleIds(List<UUID> ids){if(ids==null||ids.isEmpty())return List.of(); Set<UUID> seen=new HashSet<>(); for(UUID id:ids)if(id==null||!seen.add(id))throw new IllegalArgumentException("Role ids must be unique and non-null"); List<TenantRole> found=repo.findAllByIds(ids); if(found.size()!=ids.size())throw new IllegalArgumentException("One or more tenant roles do not exist"); Map<UUID,TenantRole> by=found.stream().collect(java.util.stream.Collectors.toMap(TenantRole::id,r->r)); List<TenantRole> result=new ArrayList<>(); for(UUID id:ids){TenantRole role=by.get(id);ownership.assertRoleAssignable(role);result.add(role);} return result;}
}
