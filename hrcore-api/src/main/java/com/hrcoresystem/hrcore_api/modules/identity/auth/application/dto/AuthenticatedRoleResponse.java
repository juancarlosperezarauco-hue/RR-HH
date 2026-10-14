package com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto;
import java.util.UUID;
public record AuthenticatedRoleResponse(UUID id, String code, String name, boolean systemRole) {}
