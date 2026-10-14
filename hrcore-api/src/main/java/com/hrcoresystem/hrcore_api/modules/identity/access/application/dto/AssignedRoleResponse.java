package com.hrcoresystem.hrcore_api.modules.identity.access.application.dto;
import java.util.UUID;
public record AssignedRoleResponse(UUID id,String code,String name,boolean active) {}
