package com.hrcoresystem.hrcore_api.modules.tenant.attendance.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.dto.AttendanceRequests;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.dto.AttendanceResponses;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service.AttendanceAdjustmentService;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model.AdjustmentType;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model.ReviewStatus;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attendance/adjustments")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Attendance adjustments")
public class AttendanceAdjustmentController {
    private final AttendanceAdjustmentService service;
    public AttendanceAdjustmentController(AttendanceAdjustmentService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAuthority('attendance.adjustments.create')")
    public ApiResponse<AttendanceResponses.Adjustment> create(@Valid @RequestBody AttendanceRequests.Adjustment request) {
        return ApiResponse.success("Attendance adjustment requested", service.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('attendance.adjustments.list')")
    public ApiResponse<Page<AttendanceResponses.Adjustment>> list(@RequestParam(required = false) UUID recordId,
            @RequestParam(required = false) UUID requestedBy, @RequestParam(required = false) AdjustmentType type,
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return ApiResponse.success("Attendance adjustments retrieved",
                service.list(recordId, requestedBy, type, status, page, size, sort, direction));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('attendance.adjustments.detail')")
    public ApiResponse<AttendanceResponses.Adjustment> detail(@PathVariable UUID id) {
        return ApiResponse.success("Attendance adjustment retrieved", service.detail(id));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('attendance.adjustments.approve')")
    public ApiResponse<AttendanceResponses.Adjustment> approve(@PathVariable UUID id,
            @Valid @RequestBody(required = false) AttendanceRequests.Review request) {
        return ApiResponse.success("Attendance adjustment approved", service.approve(id, request == null ? null : request.comment()));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('attendance.adjustments.reject')")
    public ApiResponse<AttendanceResponses.Adjustment> reject(@PathVariable UUID id,
            @Valid @RequestBody(required = false) AttendanceRequests.Review request) {
        return ApiResponse.success("Attendance adjustment rejected", service.reject(id, request == null ? null : request.comment()));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('attendance.adjustments.cancel')")
    public ApiResponse<AttendanceResponses.Adjustment> cancel(@PathVariable UUID id) {
        return ApiResponse.success("Attendance adjustment cancelled", service.cancel(id));
    }
}
