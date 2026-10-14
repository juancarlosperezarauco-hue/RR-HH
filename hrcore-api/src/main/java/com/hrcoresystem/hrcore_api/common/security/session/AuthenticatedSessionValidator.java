package com.hrcoresystem.hrcore_api.common.security.session;

import com.hrcoresystem.hrcore_api.common.security.principal.AuthenticatedUserPrincipal;
import com.hrcoresystem.hrcore_api.common.security.session.domain.repository.*;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import com.hrcoresystem.hrcore_api.modules.platform.superadmin.domain.repository.PlatformSuperadminRepository;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class AuthenticatedSessionValidator {
  private final TenantAuthSessionRepository tenant;
  private final PlatformAuthSessionRepository platform;
  private final TenantUserRepository users;
  private final PlatformSuperadminRepository superadmins;

  public AuthenticatedSessionValidator(TenantAuthSessionRepository tenant, PlatformAuthSessionRepository platform,
      TenantUserRepository users, PlatformSuperadminRepository superadmins) {
    this.tenant = tenant;
    this.platform = platform;
    this.users = users;
    this.superadmins = superadmins;
  }

  public void validate(AuthenticatedUserPrincipal p) {
    var s = "TENANT".equals(p.scope()) ? tenant.findById(p.sessionId()).orElseThrow()
        : platform.findById(p.sessionId()).orElseThrow();
    if (s.revokedAt() != null || !s.expiresAt().isAfter(Instant.now()) || !s.userId().equals(p.subjectId()))
      throw new IllegalStateException("Session is not active");
    if ("TENANT".equals(p.scope())) {
      var u = users.findById(p.subjectId()).orElseThrow();
      if (!u.active() || u.securityVersion() != p.securityVersion())
        throw new IllegalStateException("Authorization is stale");
    } else {
      var u = superadmins.findById(p.subjectId()).orElseThrow();
      if (!u.active() || u.securityVersion() != p.securityVersion())
        throw new IllegalStateException("Authorization is stale");
    }
  }
}
