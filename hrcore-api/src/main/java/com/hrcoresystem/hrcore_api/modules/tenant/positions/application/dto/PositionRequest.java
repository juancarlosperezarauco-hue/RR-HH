package com.hrcoresystem.hrcore_api.modules.tenant.positions.application.dto;

import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.model.OrganizationalLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = false)
public record PositionRequest(
                              @NotBlank @Size(max = 150) String name,
                              String description, String responsibilities,
                              @NotNull OrganizationalLevel organizationalLevel,
                              BigDecimal salaryMin, BigDecimal salaryMax, String currency) { public PositionRequest(String ignoredCode,String name,String description,String responsibilities,OrganizationalLevel organizationalLevel,BigDecimal salaryMin,BigDecimal salaryMax,String currency){this(name,description,responsibilities,organizationalLevel,salaryMin,salaryMax,currency);} }
