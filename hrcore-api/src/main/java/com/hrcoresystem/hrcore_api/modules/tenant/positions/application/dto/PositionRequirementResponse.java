package com.hrcoresystem.hrcore_api.modules.tenant.positions.application.dto;

import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.model.RequirementType;
import java.time.Instant;
import java.util.UUID;

public record PositionRequirementResponse(UUID id, UUID positionId, RequirementType requirementType,
                                          String description, boolean mandatory, Integer minimumYearsExperience,
                                          Instant createdAt, Instant updatedAt) { }
