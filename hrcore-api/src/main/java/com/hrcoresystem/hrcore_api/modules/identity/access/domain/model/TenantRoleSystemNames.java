package com.hrcoresystem.hrcore_api.modules.identity.access.domain.model;

public final class TenantRoleSystemNames {

  public static final String OWNER_ADMIN = "OWNER_ADMIN";

  private TenantRoleSystemNames() {
  }

  public static boolean isReservedCode(String code) {
    return OWNER_ADMIN.equals(code);
  }
}
