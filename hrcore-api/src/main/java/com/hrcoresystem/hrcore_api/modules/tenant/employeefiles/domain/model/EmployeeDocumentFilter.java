package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model;

import java.time.LocalDate;

public record EmployeeDocumentFilter(
    DocumentType type, FileStatus status, LocalDate expiresBefore, boolean includeConfidential) {}
