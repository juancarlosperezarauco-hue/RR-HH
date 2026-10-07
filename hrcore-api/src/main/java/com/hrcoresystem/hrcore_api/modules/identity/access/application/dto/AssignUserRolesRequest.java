package com.hrcoresystem.hrcore_api.modules.identity.access.application.dto;

import java.util.List;
import java.util.UUID;

public record AssignUserRolesRequest(
        List<UUID> roleIds
) {
}