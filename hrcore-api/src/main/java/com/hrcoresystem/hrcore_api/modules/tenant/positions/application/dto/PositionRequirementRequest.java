package com.hrcoresystem.hrcore_api.modules.tenant.positions.application.dto;

import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.model.RequirementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PositionRequirementRequest(@NotNull RequirementType requirementType,
                                         @NotBlank String description, @NotNull Boolean mandatory,
                                         Integer minimumYearsExperience) { }
