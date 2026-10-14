package com.hrcoresystem.hrcore_api.common.security.jwt;
import java.time.Instant; import java.util.List; import java.util.UUID;
public record AccessTokenPayload(UUID subjectId,UUID sessionId,String tenantSlug,String scope,String email,String displayName,List<String> roles,List<String> permissions,long securityVersion,Long authorizationVersion,long catalogVersion,Instant issuedAt,Instant expiresAt,UUID tokenId) {}
