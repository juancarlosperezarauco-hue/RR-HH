package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.dto;

import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model.VerificationStatus;
import jakarta.validation.constraints.NotNull;

public record VerificationRequest(@NotNull VerificationStatus status) {}
