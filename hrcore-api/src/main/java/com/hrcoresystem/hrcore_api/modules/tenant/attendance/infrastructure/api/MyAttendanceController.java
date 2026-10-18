package com.hrcoresystem.hrcore_api.modules.tenant.attendance.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.dto.AttendanceRequests;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.dto.AttendanceResponses;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.repository.AttendanceStore;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me/attendance")
public class MyAttendanceController {
    private final AttendanceActor actor;
    private final AttendanceStore store;
    private final AttendancePunchService punches;
    private final AttendanceCalculationService records;
    private final AttendanceAdjustmentService adjustments;
    private final AttendanceOvertimeService overtime;

    public MyAttendanceController(AttendanceActor actor, AttendanceStore store, AttendancePunchService punches,
            AttendanceCalculationService records, AttendanceAdjustmentService adjustments,
            AttendanceOvertimeService overtime) {
        this.actor = actor; this.store = store; this.punches = punches; this.records = records;
        this.adjustments = adjustments; this.overtime = overtime;
    }

    @PostMapping("/punches")
    @PreAuthorize("hasAuthority('attendance.punches.create')")
    public ApiResponse<AttendanceResponses.Punch> punch(@Valid @RequestBody AttendanceRequests.SelfAttendancePunchRequest request) {
        return ApiResponse.success("Marcación registrada correctamente", punches.create(
                new AttendanceRequests.Punch(null, request.punchType(), null, request.latitude(), request.longitude(), request.notes())));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('attendance.records.list')")
    public ApiResponse<Page<AttendanceResponses.Record>> records(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) AttendanceStatus status, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size, @RequestParam(defaultValue = "workDate") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return ApiResponse.success("Asistencia consultada correctamente", records.list(currentEmployee(), from, to, status, page, size, sort, direction));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('attendance.records.detail')")
    public ApiResponse<AttendanceResponses.Record> record(@PathVariable UUID id) {
        var record = records.detail(id);
        if (!record.employeeId().equals(currentEmployee())) throw new org.springframework.security.access.AccessDeniedException("El registro no pertenece al empleado autenticado");
        return ApiResponse.success("Asistencia consultada correctamente", record);
    }

    @GetMapping("/punches")
    @PreAuthorize("hasAuthority('attendance.punches.list')")
    public ApiResponse<Page<AttendanceResponses.Punch>> punchList(@RequestParam(required = false) UUID recordId,
            @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
            @RequestParam(required = false) PunchType type, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.success("Marcaciones consultadas correctamente", punches.list(currentEmployee(), recordId, from, to, type, null, page, size, "punchedAt", "desc"));
    }

    @PostMapping("/adjustments")
    @PreAuthorize("hasAuthority('attendance.adjustments.create')")
    public ApiResponse<AttendanceResponses.Adjustment> adjustment(@Valid @RequestBody AttendanceRequests.Adjustment request) {
        var record = records.detail(request.attendanceRecordId());
        if (!record.employeeId().equals(currentEmployee())) throw new org.springframework.security.access.AccessDeniedException("El registro no pertenece al empleado autenticado");
        return ApiResponse.success("Solicitud de corrección registrada correctamente", adjustments.create(request));
    }

    @GetMapping("/adjustments")
    @PreAuthorize("hasAuthority('attendance.adjustments.list')")
    public ApiResponse<Page<AttendanceResponses.Adjustment>> adjustmentList(@RequestParam(required = false) ReviewStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.success("Solicitudes de corrección consultadas correctamente", adjustments.list(null, actor.id(), null, status, page, size, "createdAt", "desc"));
    }

    @PostMapping("/overtime")
    @PreAuthorize("hasAuthority('attendance.overtime.create')")
    public ApiResponse<AttendanceResponses.Overtime> overtime(@Valid @RequestBody AttendanceRequests.Overtime request) {
        var record = records.detail(request.attendanceRecordId());
        if (!record.employeeId().equals(currentEmployee())) throw new org.springframework.security.access.AccessDeniedException("El registro no pertenece al empleado autenticado");
        return ApiResponse.success("Solicitud de horas extras registrada correctamente", overtime.create(request));
    }

    @GetMapping("/overtime")
    @PreAuthorize("hasAuthority('attendance.overtime.list')")
    public ApiResponse<Page<AttendanceResponses.Overtime>> overtimeList(@RequestParam(required = false) ReviewStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.success("Solicitudes de horas extras consultadas correctamente", overtime.listOwn(actor.id(), status, page, size));
    }

    private UUID currentEmployee() {
        return store.employeeForUser(actor.id()).orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("La cuenta no tiene un empleado asociado"));
    }
}
