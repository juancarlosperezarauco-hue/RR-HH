package com.hrcoresystem.hrcore_api.modules.platform.security.application;
import com.hrcoresystem.hrcore_api.common.security.authorization.AuthorizationGuards; import org.springframework.security.access.AccessDeniedException; import org.springframework.stereotype.Service;
@Service public class PlatformAdministrationPolicy { private final AuthorizationGuards guards; public PlatformAdministrationPolicy(AuthorizationGuards guards){this.guards=guards;} public void requireSuperadmin(){if(!guards.isPlatformAdmin())throw new AccessDeniedException("Platform SUPERADMIN required");} }
