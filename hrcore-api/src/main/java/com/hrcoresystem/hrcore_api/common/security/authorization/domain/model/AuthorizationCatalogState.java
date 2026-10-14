package com.hrcoresystem.hrcore_api.common.security.authorization.domain.model;

import java.time.Instant;

public record AuthorizationCatalogState(short id, long catalogVersion, Instant updatedAt) {
}
