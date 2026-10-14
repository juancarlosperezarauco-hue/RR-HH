package com.hrcoresystem.hrcore_api.common.security.jwt;
import java.time.Instant; import java.util.UUID;
public record RefreshTokenPayload(UUID subjectId,UUID sessionId,String tenantSlug,String scope,long generation,Instant issuedAt,Instant expiresAt,UUID tokenId) {}
