package com.hrcoresystem.hrcore_api.modules.tenant.departments.application.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model.DepartmentHistoryAction;
import java.time.Instant;
import java.util.UUID;

public record DepartmentHistoryResponse(
        UUID id, UUID departmentId, DepartmentHistoryAction action,
        JsonNode previousData, JsonNode newData, String reason,
        UUID performedBy, Instant createdAt
) { }
