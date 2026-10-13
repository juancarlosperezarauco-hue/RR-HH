package com.hrcoresystem.hrcore_api.modules.tenant.attendance.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.dto.AttendanceRequests;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.dto.AttendanceResponses;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service.AttendanceCalculationService;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model.AttendanceStatus;
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
@RequestMapping("/api/attendance/records")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Attendance records")
public class AttendanceRecordController {
    private final AttendanceCalculationService service;
    public AttendanceRecordController(AttendanceCalculationService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAuthority('attendance.records.list')")
    public ApiResponse<Page<AttendanceResponses.Record>> list(@RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) AttendanceStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "workDate") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return ApiResponse.success("Attendance records retrieved",
                service.list(employeeId, from, to, status, page, size, sort, direction));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('attendance.records.detail')")
    public ApiResponse<AttendanceResponses.Record> detail(@PathVariable UUID id) {
        return ApiResponse.success("Attendance record retrieved", service.detail(id));
    }

    @PostMapping("/calculate")
    @PreAuthorize("hasAuthority('attendance.records.calculate')")
    public ApiResponse<AttendanceResponses.Record> calculate(@Valid @RequestBody AttendanceRequests.Calculate request) {
        return ApiResponse.success("Attendance calculated", service.calculate(request.employeeId(), request.workDate()));
    }
}
