package com.hrcoresystem.hrcore_api.common.security.authorization;
import org.springframework.stereotype.Component;
@Component("tenantScopeGuard") public class TenantScopeGuard { private final AuthorizationGuards guards; public TenantScopeGuard(AuthorizationGuards guards){this.guards=guards;} public boolean isTenant(){return guards.isTenantAuthenticated();} }
