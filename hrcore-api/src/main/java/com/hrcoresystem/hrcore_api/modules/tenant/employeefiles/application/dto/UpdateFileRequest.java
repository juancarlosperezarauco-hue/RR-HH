package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.dto;

import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model.FileStatus;
import jakarta.validation.constraints.*;

public record UpdateFileRequest(
    String notes, @NotNull FileStatus status) {
  public UpdateFileRequest(String ignoredFileNumber, String notes, FileStatus status) {
    this(notes, status);
  }
}
