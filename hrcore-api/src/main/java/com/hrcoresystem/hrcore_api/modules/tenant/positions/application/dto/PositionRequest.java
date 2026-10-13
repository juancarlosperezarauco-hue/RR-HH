package com.hrcoresystem.hrcore_api.modules.tenant.positions.application.dto;

import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.model.OrganizationalLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PositionRequest(@NotBlank @Size(max = 50) String code,
                              @NotBlank @Size(max = 150) String name,
                              String description, String responsibilities,
                              OrganizationalLevel organizationalLevel,
                              BigDecimal salaryMin, BigDecimal salaryMax, String currency) { }
