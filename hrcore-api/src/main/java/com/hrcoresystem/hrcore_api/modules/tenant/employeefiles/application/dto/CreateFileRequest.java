package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record CreateFileRequest(
    @NotNull UUID employeeId, String notes) {
  public CreateFileRequest(UUID employeeId, String ignoredFileNumber, String notes) {
    this(employeeId, notes);
  }
}
