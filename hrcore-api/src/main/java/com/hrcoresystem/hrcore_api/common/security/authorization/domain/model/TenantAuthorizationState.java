package com.hrcoresystem.hrcore_api.common.security.authorization.domain.model;

import java.time.Instant;
import java.util.UUID;

public record TenantAuthorizationState(short id, UUID ownerUserId, long authorizationVersion, Instant updatedAt) {
}
