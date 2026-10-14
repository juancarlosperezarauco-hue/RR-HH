package com.hrcoresystem.hrcore_api.modules.identity.access.domain.model;

public record PermissionDefinition(String code, String module, String description, PermissionScope scope,
    boolean customRoleAssignable, boolean active) {
}
