package com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model;

import java.util.UUID;

public record DepartmentFilter(DepartmentStatus status, String name, String code, UUID parentDepartmentId) { }
