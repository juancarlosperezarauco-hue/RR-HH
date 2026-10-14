package com.hrcoresystem.hrcore_api.common.security.authorization.domain;

public record PermissionDefinition(String code, String module, String description,
    PermissionScope scope, boolean customRoleAssignable, boolean active) {
}
