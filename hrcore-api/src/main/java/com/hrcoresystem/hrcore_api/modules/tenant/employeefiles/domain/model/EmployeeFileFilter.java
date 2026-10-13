package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model;

import java.util.UUID;

public record EmployeeFileFilter(UUID employeeId, String fileNumber, FileStatus status) {}
