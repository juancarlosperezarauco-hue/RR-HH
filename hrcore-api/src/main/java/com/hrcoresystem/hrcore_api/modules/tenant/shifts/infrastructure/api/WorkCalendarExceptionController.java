package com.hrcoresystem.hrcore_api.modules.tenant.shifts.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.WorkCalendarExceptionRequest;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.WorkCalendarExceptionResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service.WorkCalendarExceptionService;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.CalendarExceptionType;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.WorkCalendarExceptionFilter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shifts/calendar-exceptions")
@SecurityRequirement(name = "bearerAuth")
public class WorkCalendarExceptionController {
    private static final Set<String> SORT = Set.of("exceptionDate", "exceptionType", "createdAt", "id");
    private final WorkCalendarExceptionService exceptions;

    public WorkCalendarExceptionController(WorkCalendarExceptionService exceptions) {
        this.exceptions = exceptions;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('shifts.calendar.create')")
    public ApiResponse<WorkCalendarExceptionResponse> create(@Valid @RequestBody WorkCalendarExceptionRequest request) {
        return ApiResponse.success("Calendar exception created", exceptions.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('shifts.calendar.list')")
    public ApiResponse<Page<WorkCalendarExceptionResponse>> list(@RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) CalendarExceptionType exceptionType,
            @RequestParam(required = false) Boolean workRequired,
            @RequestParam(required = false) Boolean organizationOnly,
            @ParameterObject @PageableDefault(size = 50) Pageable pageable) {
        WorkCalendarExceptionFilter filter = new WorkCalendarExceptionFilter(
                employeeId, departmentId, fromDate, toDate, exceptionType, workRequired, organizationOnly);
        return ApiResponse.success("Calendar exceptions retrieved",
                exceptions.list(filter, ShiftPageables.checked(pageable, SORT, "exceptionDate")));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('shifts.calendar.detail')")
    public ApiResponse<WorkCalendarExceptionResponse> detail(@PathVariable UUID id) {
        return ApiResponse.success("Calendar exception retrieved", exceptions.get(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('shifts.calendar.update')")
    public ApiResponse<WorkCalendarExceptionResponse> update(@PathVariable UUID id,
            @Valid @RequestBody WorkCalendarExceptionRequest request) {
        return ApiResponse.success("Calendar exception updated", exceptions.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('shifts.calendar.delete')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        exceptions.delete(id);
        return ApiResponse.success("Calendar exception deleted", null);
    }
}
