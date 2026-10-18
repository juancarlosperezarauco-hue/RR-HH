package com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.api;
import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.OrganizationChartResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase.GetOrganizationChartUseCase;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/organization") public class OrganizationChartController { private final GetOrganizationChartUseCase useCase; public OrganizationChartController(GetOrganizationChartUseCase useCase){this.useCase=useCase;} @GetMapping("/chart") @PreAuthorize("hasAuthority('employees.list') and hasAuthority('departments.list')") public ApiResponse<OrganizationChartResponse> chart(){return ApiResponse.success("Organigrama consultado correctamente",useCase.execute());} }
