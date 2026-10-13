package com.hrcoresystem.hrcore_api.modules.tenant.attendance.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.dto.AttendanceRequests;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.dto.AttendanceResponses;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service.AttendanceOvertimeService;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model.ReviewStatus;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attendance/overtime")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Attendance overtime")
public class AttendanceOvertimeController {
    private final AttendanceOvertimeService service;
    public AttendanceOvertimeController(AttendanceOvertimeService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAuthority('attendance.overtime.create')")
    public ApiResponse<AttendanceResponses.Overtime> create(@Valid @RequestBody AttendanceRequests.Overtime request) {
        return ApiResponse.success("Attendance overtime requested", service.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('attendance.overtime.list')")
    public ApiResponse<Page<AttendanceResponses.Overtime>> list(@RequestParam(required = false) UUID recordId,
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return ApiResponse.success("Attendance overtime retrieved",
                service.list(recordId, status, from, to, page, size, sort, direction));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('attendance.overtime.detail')")
    public ApiResponse<AttendanceResponses.Overtime> detail(@PathVariable UUID id) {
        return ApiResponse.success("Attendance overtime retrieved", service.detail(id));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('attendance.overtime.approve')")
    public ApiResponse<AttendanceResponses.Overtime> approve(@PathVariable UUID id,
            @Valid @RequestBody AttendanceRequests.OvertimeApproval request) {
        return ApiResponse.success("Attendance overtime approved", service.approve(id, request));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('attendance.overtime.reject')")
    public ApiResponse<AttendanceResponses.Overtime> reject(@PathVariable UUID id,
            @Valid @RequestBody(required = false) AttendanceRequests.Review request) {
        return ApiResponse.success("Attendance overtime rejected", service.reject(id, request == null ? null : request.comment()));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('attendance.overtime.cancel')")
    public ApiResponse<AttendanceResponses.Overtime> cancel(@PathVariable UUID id) {
        return ApiResponse.success("Attendance overtime cancelled", service.cancel(id));
    }
}
