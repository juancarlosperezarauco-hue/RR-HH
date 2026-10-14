package com.hrcoresystem.hrcore_api.common.security.session;

import com.hrcoresystem.hrcore_api.common.security.JwtProperties;
import com.hrcoresystem.hrcore_api.common.security.jwt.*;
import com.hrcoresystem.hrcore_api.common.security.session.domain.model.AuthSession;
import com.hrcoresystem.hrcore_api.common.security.session.domain.repository.*;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.platform.superadmin.domain.model.PlatformSuperadmin;
import com.hrcoresystem.hrcore_api.modules.platform.superadmin.domain.repository.PlatformSuperadminRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.service.TenantAuthorizationSnapshotService;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import com.hrcoresystem.hrcore_api.common.security.authorization.domain.repository.AuthorizationCatalogStateRepository;
import com.hrcoresystem.hrcore_api.common.security.authorization.domain.repository.TenantAuthorizationStateRepository;
import com.hrcoresystem.hrcore_api.common.security.authorization.domain.model.AuthorizationCatalogState;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUserStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
public class AuthSessionService {
  private final JwtTokenService jwt;
  private final JwtProperties props;
  private final TenantAuthSessionRepository tenant;
  private final PlatformAuthSessionRepository platform;
  private final TenantUserRepository tenantUsers;
  private final PlatformSuperadminRepository platformUsers;
  private final TenantAuthorizationSnapshotService snapshots;
  private final AuthorizationCatalogStateRepository catalogState;

  @Autowired
  public AuthSessionService(JwtTokenService jwt, JwtProperties props, TenantAuthSessionRepository tenant,
      PlatformAuthSessionRepository platform, TenantUserRepository tenantUsers, PlatformSuperadminRepository platformUsers,
      TenantAuthorizationSnapshotService snapshots, AuthorizationCatalogStateRepository catalogState) {
    this.jwt = jwt;
    this.props = props;
    this.tenant = tenant;
    this.platform = platform;
    this.tenantUsers = tenantUsers; this.platformUsers = platformUsers; this.snapshots = snapshots; this.catalogState = catalogState;
  }

  public AuthSessionService(JwtTokenService jwt, JwtProperties props, TenantAuthSessionRepository tenant,
      PlatformAuthSessionRepository platform) {
    this(jwt, props, tenant, platform, null, null, null, null);
  }

  @Transactional
  public IssuedTokens createTenantSession(TenantUser user, String slug) {
    var snapshot = snapshots.getSnapshot(user.id());
    return createTenantSession(user, slug, snapshot.roleCodes(), snapshot.permissionCodes(), snapshot.authorizationVersion(), snapshot.catalogVersion());
  }

  @Transactional
  public IssuedTokens refreshTenantSession(String refreshToken, String tenantSlug) {
    var parsed = jwt.parseRefreshTokenPayload(refreshToken);
    if (!"TENANT".equals(parsed.scope()) || !tenantSlug.equalsIgnoreCase(parsed.tenantSlug())) throw new AccessDeniedException("Invalid tenant refresh scope");
    var session = tenant.findById(parsed.sessionId()).orElseThrow(() -> new AccessDeniedException("Session not found"));
    if (session.revokedAt()!=null || session.expiresAt().isBefore(Instant.now()) || session.refreshGeneration()!=parsed.generation() || !SessionTokenHasher.matches(refreshToken, session.refreshTokenHash())) {
      tenant.revokeOwnedSession(session.id(), session.userId()); throw new AccessDeniedException("Refresh token reuse detected");
    }
    TenantUser user = tenantUsers.findById(parsed.subjectId()).orElseThrow(() -> new AccessDeniedException("User not found"));
    if (!user.active() || user.status()!=TenantUserStatus.ACTIVE) throw new AccessDeniedException("User inactive");
    var snapshot = snapshots.getSnapshot(user.id());
    return rotateTenant(session, user, tenantSlug, snapshot);
  }

  @Transactional
  public IssuedTokens refreshPlatformSession(String refreshToken) {
    var parsed = jwt.parseRefreshTokenPayload(refreshToken);
    if (!"PLATFORM".equals(parsed.scope()) || !"platform".equalsIgnoreCase(parsed.tenantSlug())) throw new AccessDeniedException("Invalid platform refresh scope");
    var session = platform.findById(parsed.sessionId()).orElseThrow(() -> new AccessDeniedException("Session not found"));
    if (session.revokedAt()!=null || session.expiresAt().isBefore(Instant.now()) || session.refreshGeneration()!=parsed.generation() || !SessionTokenHasher.matches(refreshToken, session.refreshTokenHash())) {
      platform.revokeOwnedSession(session.id(), session.userId()); throw new AccessDeniedException("Refresh token reuse detected");
    }
    var user = platformUsers.findById(parsed.subjectId()).orElseThrow(() -> new AccessDeniedException("Superadmin not found"));
    if (!user.active()) throw new AccessDeniedException("Superadmin inactive");
    Instant now=Instant.now(), expires=session.expiresAt(); UUID newJti=UUID.randomUUID();
    String nextRefresh=jwt.generateRefreshToken(new RefreshTokenPayload(user.id(),session.id(),"platform","PLATFORM",session.refreshGeneration()+1,now,expires,newJti));
    platform.save(new AuthSession(session.id(),session.userId(),session.tokenFamilyId(),SessionTokenHasher.hash(nextRefresh),session.refreshGeneration()+1,session.createdAt(),session.expiresAt(),now,now,null,session.userAgent(),session.ipAddress()));
    long cv=catalogState.find().orElseThrow(() -> new IllegalStateException("Authorization catalog state is missing")).catalogVersion();
    String access=jwt.generateAccessToken(new AccessTokenPayload(user.id(),session.id(),"platform","PLATFORM",user.email(),(user.firstName()+" "+user.lastName()).trim(),List.of("SUPERADMIN"),List.of(),user.securityVersion(),null,cv,now,now.plusMillis(props.getAccessExpirationMs()),UUID.randomUUID()));
    return new IssuedTokens(access,nextRefresh,props.getAccessExpirationMs());
  }

  private IssuedTokens rotateTenant(AuthSession session, TenantUser user, String slug, TenantAuthorizationSnapshotService.TenantAuthorizationSnapshot snapshot) {
    Instant now=Instant.now(); long generation=session.refreshGeneration()+1;
    String nextRefresh=jwt.generateRefreshToken(new RefreshTokenPayload(user.id(),session.id(),slug,"TENANT",generation,now,session.expiresAt(),UUID.randomUUID()));
    tenant.save(new AuthSession(session.id(),session.userId(),session.tokenFamilyId(),SessionTokenHasher.hash(nextRefresh),generation,session.createdAt(),session.expiresAt(),now,now,null,session.userAgent(),session.ipAddress()));
    String access=jwt.generateAccessToken(new AccessTokenPayload(user.id(),session.id(),slug,"TENANT",user.email(),(user.firstName()+" "+user.lastName()).trim(),snapshot.roleCodes(),snapshot.permissionCodes(),snapshot.securityVersion(),snapshot.authorizationVersion(),snapshot.catalogVersion(),now,now.plusMillis(props.getAccessExpirationMs()),UUID.randomUUID()));
    return new IssuedTokens(access,nextRefresh,props.getAccessExpirationMs());
  }

  @Transactional
  public IssuedTokens createTenantSession(TenantUser u, String slug, List<String> roles, List<String> permissions,
      long av, long cv) {
    return create(u.id(), u.email(), u.firstName(), u.lastName(), slug, "TENANT", roles, permissions,
        u.securityVersion(), av, cv, false);
  }

  @Transactional
  public IssuedTokens createPlatformSession(PlatformSuperadmin u) {
    return create(u.id(), u.email(), u.firstName(), u.lastName(), "platform", "PLATFORM", List.of("SUPERADMIN"),
        List.of(), u.securityVersion(), 0, 1, true);
  }

  private IssuedTokens create(UUID subject, String email, String first, String last, String slug, String scope,
      List<String> roles, List<String> perms, long sv, long av, long cv, boolean isPlatform) {
    Instant now = Instant.now();
    UUID sid = UUID.randomUUID(), family = UUID.randomUUID(), jti = UUID.randomUUID();
    Instant exp = now.plusMillis(props.getAccessExpirationMs()),
        sessionExp = now.plusMillis(props.getRefreshExpirationMs());
    String refresh = jwt
        .generateRefreshToken(new RefreshTokenPayload(subject, sid, slug, scope, 1, now, sessionExp, jti));
    String access = jwt.generateAccessToken(new AccessTokenPayload(subject, sid, slug, scope, email,
        (first + " " + last).trim(), roles, perms, sv, isPlatform ? null : av, cv, now, exp, UUID.randomUUID()));
    AuthSession s = new AuthSession(sid, subject, family, SessionTokenHasher.hash(refresh), 1, now, sessionExp, now,
        now, null, null, null);
    if (isPlatform)
      platform.save(s);
    else
      tenant.save(s);
    return new IssuedTokens(access, refresh, props.getAccessExpirationMs());
  }

  public void revokeTenantSession(UUID id) {
    tenant.findById(id)
        .ifPresent(s -> tenant.save(new AuthSession(s.id(), s.userId(), s.tokenFamilyId(), s.refreshTokenHash(),
            s.refreshGeneration(), s.createdAt(), s.expiresAt(), s.lastUsedAt(), s.lastRotatedAt(), Instant.now(),
            s.userAgent(), s.ipAddress())));
  }

  public void revokePlatformSession(UUID id) {
    platform.findById(id)
        .ifPresent(s -> platform.save(new AuthSession(s.id(), s.userId(), s.tokenFamilyId(), s.refreshTokenHash(),
            s.refreshGeneration(), s.createdAt(), s.expiresAt(), s.lastUsedAt(), s.lastRotatedAt(), Instant.now(),
            s.userAgent(), s.ipAddress())));
  }

  @Transactional
  public void revokeAllTenantSessions(UUID userId) {
    tenant.findByUserId(userId).forEach(s -> tenant.revokeOwnedSession(s.id(), userId));
  }

  @Transactional
  public void revokeAllPlatformSessions(UUID superadminId) {
    platform.findByUserId(superadminId).forEach(s -> platform.revokeOwnedSession(s.id(), superadminId));
  }
}
