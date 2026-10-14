package com.hrcoresystem.hrcore_api.modules.identity.access.infrastructure.security;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.policy.TenantOwnershipPolicy;
import org.springframework.stereotype.Component;
@Component("tenantOwnershipGuard") public class TenantOwnershipGuard { private final TenantOwnershipPolicy policy; public TenantOwnershipGuard(TenantOwnershipPolicy policy){this.policy=policy;} public boolean isCurrentOwner(){return policy.isCurrentOwner();} }
