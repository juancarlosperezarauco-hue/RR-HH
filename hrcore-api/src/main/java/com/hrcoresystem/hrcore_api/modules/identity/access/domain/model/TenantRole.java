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
    Instant updatedAt,
    TenantRoleType roleType) {

  public boolean isSystemRole() {
    return systemRole;
  }

  public boolean isOwnerAdmin() {
    return roleType == TenantRoleType.ROOT
        && systemRole
        && "OWNER_ADMIN".equals(code);
  }
}
