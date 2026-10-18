package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
public record TransferEmployeeRequest(@NotNull UUID departmentId, @NotNull UUID positionId,
        UUID supervisorEmployeeId, @NotNull LocalDate effectiveDate,
        @NotBlank @Size(max = 1000) String reason) { }
