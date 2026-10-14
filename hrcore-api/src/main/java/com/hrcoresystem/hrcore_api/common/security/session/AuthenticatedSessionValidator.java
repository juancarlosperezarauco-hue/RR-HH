package com.hrcoresystem.hrcore_api.common.security.session;

import com.hrcoresystem.hrcore_api.common.security.principal.AuthenticatedUserPrincipal;
import com.hrcoresystem.hrcore_api.common.security.session.domain.repository.*;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import com.hrcoresystem.hrcore_api.modules.platform.superadmin.domain.repository.PlatformSuperadminRepository;
import com.hrcoresystem.hrcore_api.common.security.authorization.domain.repository.TenantAuthorizationStateRepository;
import com.hrcoresystem.hrcore_api.common.security.authorization.domain.repository.AuthorizationCatalogStateRepository;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUserStatus;
import com.hrcoresystem.hrcore_api.modules.platform.auth.domain.model.PlatformAuthConstants;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class AuthenticatedSessionValidator {
  private final TenantAuthSessionRepository tenant;
  private final PlatformAuthSessionRepository platform;
  private final TenantUserRepository users;
  private final PlatformSuperadminRepository superadmins;
  private final TenantAuthorizationStateRepository tenantAuthorizationState;
  private final AuthorizationCatalogStateRepository catalogState;

  public AuthenticatedSessionValidator(TenantAuthSessionRepository tenant, PlatformAuthSessionRepository platform,
      TenantUserRepository users, PlatformSuperadminRepository superadmins,
      TenantAuthorizationStateRepository tenantAuthorizationState,
      AuthorizationCatalogStateRepository catalogState) {
    this.tenant = tenant;
    this.platform = platform;
    this.users = users;
    this.superadmins = superadmins;
    this.tenantAuthorizationState = tenantAuthorizationState;
    this.catalogState = catalogState;
  }

  public void validate(AuthenticatedUserPrincipal p) {
    if (p == null || p.sessionId() == null || p.subjectId() == null)
      throw new BadCredentialsException("Invalid session");
    if ("TENANT".equals(p.scope())) {
      var context = TenantContextHolder.getRequired();
      if (context.publicRequest() || !context.tenantSlug().equals(p.tenantSlug()))
        throw new BadCredentialsException("Invalid tenant scope");
      var s = tenant.findById(p.sessionId()).orElseThrow(() -> new BadCredentialsException("Invalid session"));
      if (s.revokedAt() != null || !s.expiresAt().isAfter(Instant.now()) || !s.userId().equals(p.subjectId()))
        throw new BadCredentialsException("Invalid session");
      var u = users.findById(p.subjectId()).orElseThrow();
      var auth = tenantAuthorizationState.find()
          .orElseThrow(() -> new IllegalStateException("Tenant authorization state is missing"));
      var cat = catalogState.find()
          .orElseThrow(() -> new IllegalStateException("Authorization catalog state is missing"));
      if (!u.active() || u.status() != TenantUserStatus.ACTIVE || u.securityVersion() != p.securityVersion()
          || p.authorizationVersion() == null || p.authorizationVersion() != auth.authorizationVersion()
          || p.catalogVersion() != cat.catalogVersion())
        throw new BadCredentialsException("Authorization is stale");
    } else if ("PLATFORM".equals(p.scope())) {
      if (!PlatformAuthConstants.PLATFORM_TENANT_SLUG.equals(p.tenantSlug()))
        throw new BadCredentialsException("Invalid platform scope");
      var s = platform.findById(p.sessionId()).orElseThrow(() -> new BadCredentialsException("Invalid session"));
      if (s.revokedAt() != null || !s.expiresAt().isAfter(Instant.now()) || !s.userId().equals(p.subjectId()))
        throw new BadCredentialsException("Invalid session");
      var u = superadmins.findById(p.subjectId()).orElseThrow();
      var cat = catalogState.find()
          .orElseThrow(() -> new IllegalStateException("Authorization catalog state is missing"));
      if (!u.active() || u.securityVersion() != p.securityVersion() || p.catalogVersion() != cat.catalogVersion())
        throw new BadCredentialsException("Authorization is stale");
    } else
      throw new BadCredentialsException("Invalid scope");
  }
}
