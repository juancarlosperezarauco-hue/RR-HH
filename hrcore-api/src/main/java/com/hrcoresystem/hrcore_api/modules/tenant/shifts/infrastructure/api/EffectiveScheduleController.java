package com.hrcoresystem.hrcore_api.modules.tenant.shifts.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.ScheduleResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service.EffectiveScheduleService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shifts/schedule")
@SecurityRequirement(name = "bearerAuth")
public class EffectiveScheduleController {
    private final EffectiveScheduleService schedules;

    public EffectiveScheduleController(EffectiveScheduleService schedules) {
        this.schedules = schedules;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('shifts.schedule.read')")
    public ApiResponse<ScheduleResponse> get(@RequestParam UUID employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success("Effective schedule retrieved", schedules.get(employeeId, date));
    }
}
