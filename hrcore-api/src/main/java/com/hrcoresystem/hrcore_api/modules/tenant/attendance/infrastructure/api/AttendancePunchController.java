package com.hrcoresystem.hrcore_api.modules.tenant.attendance.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.dto.AttendanceRequests;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.dto.AttendanceResponses;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service.AttendancePunchService;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model.PunchSource;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model.PunchType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attendance/punches")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Attendance punches")
public class AttendancePunchController {
    private final AttendancePunchService service;
    public AttendancePunchController(AttendancePunchService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('attendance.punches.create','attendance.punches.manage')")
    public ApiResponse<AttendanceResponses.Punch> create(@Valid @RequestBody AttendanceRequests.Punch request) {
        return ApiResponse.success("Attendance punch recorded", service.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('attendance.punches.list')")
    public ApiResponse<Page<AttendanceResponses.Punch>> list(@RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) UUID recordId, @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to, @RequestParam(required = false) PunchType type,
            @RequestParam(required = false) PunchSource source,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "punchedAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return ApiResponse.success("Attendance punches retrieved",
                service.list(employeeId, recordId, from, to, type, source, page, size, sort, direction));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('attendance.punches.detail')")
    public ApiResponse<AttendanceResponses.Punch> detail(@PathVariable UUID id) {
        return ApiResponse.success("Attendance punch retrieved", service.detail(id));
    }
}
