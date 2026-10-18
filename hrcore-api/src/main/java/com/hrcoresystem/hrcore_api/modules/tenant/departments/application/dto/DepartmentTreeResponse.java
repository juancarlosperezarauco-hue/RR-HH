package com.hrcoresystem.hrcore_api.modules.tenant.departments.application.dto;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model.DepartmentStatus; import java.util.*;
public record DepartmentTreeResponse(UUID id,String code,String name,DepartmentStatus status,UUID parentDepartmentId,UUID managerEmployeeId,List<DepartmentTreeResponse> children) {}
