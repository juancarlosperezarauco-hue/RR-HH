package com.hrcoresystem.hrcore_api.modules.tenant.shifts.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.ScheduleRangeResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.ScheduleResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service.EffectiveScheduleService;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.TenantTimezoneService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me/schedule")
@SecurityRequirement(name = "bearerAuth")
public class MyScheduleController {
    private final EffectiveScheduleService schedules;
    private final EmployeeRepository employees;
    private final SecurityContextFacade security;
    private final TenantTimezoneService timezone;

    public MyScheduleController(EffectiveScheduleService schedules, EmployeeRepository employees,
            SecurityContextFacade security, TenantTimezoneService timezone) {
        this.schedules = schedules; this.employees = employees; this.security = security; this.timezone = timezone;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('shifts.schedule.self.read')")
    public ApiResponse<ScheduleResponse> get(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success("Own schedule retrieved", schedules.get(employeeId(), date == null ? timezone.today() : date));
    }

    @GetMapping("/range")
    @PreAuthorize("hasAuthority('shifts.schedule.self.read')")
    public ApiResponse<ScheduleRangeResponse> range(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ApiResponse.success("Own schedules retrieved", schedules.range(employeeId(), fromDate, toDate));
    }

    private UUID employeeId() {
        UUID userId = security.getCurrentUserId();
        if (userId == null) throw new ResourceNotFoundException("Authenticated tenant user not found");
        return employees.findByTenantUserId(userId)
                .map(e -> e.id())
                .orElseThrow(() -> new ResourceNotFoundException("No employee is linked to the authenticated user"));
    }
}
