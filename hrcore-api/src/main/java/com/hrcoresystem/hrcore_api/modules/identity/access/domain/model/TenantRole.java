package com.hrcoresystem.hrcore_api.modules.identity.access.domain.model;

import java.time.Instant;
import java.util.UUID;

public record TenantRole(
    UUID id,
    String code,
    String name,
    String description,
    boolean systemRole,
    boolean active,
    Instant createdAt,
    Instant updatedAt) {

  public boolean isSystemRole() {
    return systemRole;
  }

  public boolean isOwnerAdmin() {
    return systemRole && "OWNER_ADMIN".equals(code);
  }
}
