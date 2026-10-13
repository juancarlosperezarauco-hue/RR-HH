package com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.model;

import java.time.Instant;
import java.util.UUID;

public record PositionRequirement(UUID id, UUID positionId, RequirementType requirementType,
                                  String description, boolean mandatory, Integer minimumYearsExperience,
                                  Instant createdAt, Instant updatedAt) { }
