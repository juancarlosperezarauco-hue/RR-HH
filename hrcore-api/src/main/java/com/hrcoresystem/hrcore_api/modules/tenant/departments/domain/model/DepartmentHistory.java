package com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record DepartmentHistory(
        UUID id, UUID departmentId, DepartmentHistoryAction action,
        JsonNode previousData, JsonNode newData, String reason,
        UUID performedBy, Instant createdAt
) { }
