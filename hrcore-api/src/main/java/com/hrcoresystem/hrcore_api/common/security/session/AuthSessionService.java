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
import org.springframework.security.authentication.BadCredentialsException;
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
      PlatformAuthSessionRepository platform, TenantUserRepository tenantUsers,
      PlatformSuperadminRepository platformUsers,
      TenantAuthorizationSnapshotService snapshots, AuthorizationCatalogStateRepository catalogState) {
    this.jwt = jwt;
    this.props = props;
    this.tenant = tenant;
    this.platform = platform;
    this.tenantUsers = tenantUsers;
    this.platformUsers = platformUsers;
    this.snapshots = snapshots;
    this.catalogState = catalogState;
  }

  @Transactional
  public IssuedTokens createTenantSession(TenantUser user, String slug) {
    var snapshot = snapshots.getSnapshot(user.id());
    return createTenantSession(user, slug, snapshot.roleCodes(), snapshot.permissionCodes(),
        snapshot.authorizationVersion(), snapshot.catalogVersion());
  }

  @Transactional(noRollbackFor = BadCredentialsException.class)
  public IssuedTokens refreshTenantSession(String refreshToken, String tenantSlug) {
    var parsed = jwt.parseRefreshTokenPayload(refreshToken);
    if (!"TENANT".equals(parsed.scope()) || !tenantSlug.equals(parsed.tenantSlug()))
      throw new BadCredentialsException("Invalid refresh token");
    var session = tenant.findByIdForUpdate(parsed.sessionId())
        .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
    if (!session.userId().equals(parsed.subjectId()) || session.revokedAt() != null
        || !session.expiresAt().isAfter(Instant.now()) || session.refreshGeneration() != parsed.generation()
        || !SessionTokenHasher.matches(refreshToken, session.refreshTokenHash())) {
      tenant.revokeOwnedSession(session.id(), session.userId());
      throw new BadCredentialsException("Invalid refresh token");
    }
    TenantUser user = tenantUsers.findById(parsed.subjectId())
        .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
    if (!user.active() || user.status() != TenantUserStatus.ACTIVE)
      throw new BadCredentialsException("Invalid refresh token");
    var snapshot = snapshots.getSnapshot(user.id());
    return rotateTenant(session, user, tenantSlug, snapshot);
  }

  @Transactional(noRollbackFor = BadCredentialsException.class)
  public IssuedTokens refreshPlatformSession(String refreshToken) {
    var parsed = jwt.parseRefreshTokenPayload(refreshToken);
    if (!"PLATFORM".equals(parsed.scope()) || !"platform".equalsIgnoreCase(parsed.tenantSlug()))
      throw new BadCredentialsException("Invalid refresh token");
    var session = platform.findByIdForUpdate(parsed.sessionId())
        .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
    if (!session.userId().equals(parsed.subjectId()) || session.revokedAt() != null
        || !session.expiresAt().isAfter(Instant.now()) || session.refreshGeneration() != parsed.generation()
        || !SessionTokenHasher.matches(refreshToken, session.refreshTokenHash())) {
      platform.revokeOwnedSession(session.id(), session.userId());
      throw new BadCredentialsException("Invalid refresh token");
    }
    var user = platformUsers.findById(parsed.subjectId())
        .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
    if (!user.active())
      throw new BadCredentialsException("Invalid refresh token");
    Instant now = Instant.now(), expires = session.expiresAt();
    UUID newJti = UUID.randomUUID();
    String nextRefresh = jwt.generateRefreshToken(new RefreshTokenPayload(user.id(), session.id(), "platform",
        "PLATFORM", session.refreshGeneration() + 1, now, expires, newJti));
    if (platform.rotate(session.id(), session.userId(), session.refreshGeneration(), session.refreshTokenHash(),
        SessionTokenHasher.hash(nextRefresh), session.refreshGeneration() + 1, now) != 1)
      throw new BadCredentialsException("Invalid refresh token");
    long cv = catalogState.find().orElseThrow(() -> new IllegalStateException("Authorization catalog state is missing"))
        .catalogVersion();
    String access = jwt.generateAccessToken(new AccessTokenPayload(user.id(), session.id(), "platform", "PLATFORM",
        user.email(), (user.firstName() + " " + user.lastName()).trim(), List.of("SUPERADMIN"), List.of(),
        user.securityVersion(), null, cv, now, now.plusMillis(props.getAccessExpirationMs()), UUID.randomUUID()));
    return new IssuedTokens(access, nextRefresh, props.getAccessExpirationMs());
  }

  private IssuedTokens rotateTenant(AuthSession session, TenantUser user, String slug,
      TenantAuthorizationSnapshotService.TenantAuthorizationSnapshot snapshot) {
    Instant now = Instant.now();
    long generation = session.refreshGeneration() + 1;
    String nextRefresh = jwt.generateRefreshToken(new RefreshTokenPayload(user.id(), session.id(), slug, "TENANT",
        generation, now, session.expiresAt(), UUID.randomUUID()));
    if (tenant.rotate(session.id(), session.userId(), session.refreshGeneration(), session.refreshTokenHash(),
        SessionTokenHasher.hash(nextRefresh), generation, now) != 1)
      throw new BadCredentialsException("Invalid refresh token");
    String access = jwt.generateAccessToken(new AccessTokenPayload(user.id(), session.id(), slug, "TENANT",
        user.email(), (user.firstName() + " " + user.lastName()).trim(), snapshot.roleCodes(),
        snapshot.permissionCodes(), snapshot.securityVersion(), snapshot.authorizationVersion(),
        snapshot.catalogVersion(), now, now.plusMillis(props.getAccessExpirationMs()), UUID.randomUUID()));
    return new IssuedTokens(access, nextRefresh, props.getAccessExpirationMs());
  }

  @Transactional
  public IssuedTokens createTenantSession(TenantUser u, String slug, List<String> roles, List<String> permissions,
      long av, long cv) {
    return create(u.id(), u.email(), u.firstName(), u.lastName(), slug, "TENANT", roles, permissions,
        u.securityVersion(), av, cv, false);
  }

  @Transactional
  public IssuedTokens createPlatformSession(PlatformSuperadmin u) {
    long cv = catalogState.find().orElseThrow(() -> new IllegalStateException("Authorization catalog state is missing"))
        .catalogVersion();
    return create(u.id(), u.email(), u.firstName(), u.lastName(), "platform", "PLATFORM", List.of("SUPERADMIN"),
        List.of(), u.securityVersion(), 0, cv, true);
  }

  private IssuedTokens create(UUID subject, String email, String first, String last, String slug, String scope,
      List<String> roles, List<String> perms, long sv, long av, long cv, boolean isPlatform) {
    Instant now = Instant.now();
    UUID sid = UUID.randomUUID(), family = UUID.randomUUID(), jti = UUID.randomUUID();
    Instant sessionExp = now.plusMillis(props.getRefreshExpirationMs());
    Instant exp = now.plusMillis(props.getAccessExpirationMs());
    String refresh = jwt
        .generateRefreshToken(new RefreshTokenPayload(subject, sid, slug, scope, 1, now, sessionExp, jti));
    exp = exp.isAfter(sessionExp) ? sessionExp : exp;
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

  @Transactional
  public void revokeTenantSession(UUID id, UUID userId) {
    if (tenant.revokeOwnedSession(id, userId) != 1)
      throw new BadCredentialsException("Invalid session");
  }

  @Transactional
  public void revokePlatformSession(UUID id, UUID userId) {
    if (platform.revokeOwnedSession(id, userId) != 1)
      throw new BadCredentialsException("Invalid session");
  }

  @Transactional
  public void revokeAllTenantSessions(UUID userId) {
    tenant.revokeAllByUserId(userId);
  }

  @Transactional
  public void revokeAllPlatformSessions(UUID superadminId) {
    platform.revokeAllByUserId(superadminId);
  }
}
