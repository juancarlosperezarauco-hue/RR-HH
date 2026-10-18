package com.hrcoresystem.hrcore_api.modules.tenant.shifts.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.ShiftBreakRequest;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.ShiftBreakResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.WorkShiftRequest;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.WorkShiftResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service.ShiftBreakService;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service.WorkShiftService;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.ShiftStatus;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/shifts")
@SecurityRequirement(name = "bearerAuth")
public class WorkShiftController {
    private final WorkShiftService shifts;
    private final ShiftBreakService breaks;

    public WorkShiftController(WorkShiftService shifts, ShiftBreakService breaks) {
        this.shifts = shifts;
        this.breaks = breaks;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('shifts.create')")
    public ApiResponse<WorkShiftResponse> create(@Valid @RequestBody WorkShiftRequest request) {
        return ApiResponse.success("Turno creado correctamente", shifts.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('shifts.list')")
    public ApiResponse<Page<WorkShiftResponse>> list(@RequestParam(required = false) String code,
            @RequestParam(required = false) String name, @RequestParam(required = false) ShiftStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "name") String sort, @RequestParam(defaultValue = "asc") String direction) {
        return ApiResponse.success("Turnos consultados correctamente", shifts.list(code, name, status, page, size, sort, direction));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('shifts.detail')")
    public ApiResponse<WorkShiftResponse> detail(@PathVariable UUID id) {
        return ApiResponse.success("Turno consultado correctamente", shifts.detail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('shifts.update')")
    public ApiResponse<WorkShiftResponse> update(@PathVariable UUID id, @Valid @RequestBody WorkShiftRequest request) {
        return ApiResponse.success("Turno actualizado correctamente", shifts.update(id, request));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('shifts.activate')")
    public ApiResponse<WorkShiftResponse> activate(@PathVariable UUID id) {
        return ApiResponse.success("Turno activado correctamente", shifts.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('shifts.deactivate')")
    public ApiResponse<WorkShiftResponse> deactivate(@PathVariable UUID id) {
        return ApiResponse.success("Turno desactivado correctamente", shifts.deactivate(id));
    }

    @PostMapping("/{id}/breaks")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('shifts.breaks.create')")
    public ApiResponse<ShiftBreakResponse> createBreak(@PathVariable UUID id, @Valid @RequestBody ShiftBreakRequest request) {
        return ApiResponse.success("Descanso creado correctamente", breaks.create(id, request));
    }

    @GetMapping("/{id}/breaks")
    @PreAuthorize("hasAuthority('shifts.breaks.list')")
    public ApiResponse<Page<ShiftBreakResponse>> listBreaks(@PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "startTime") String sort, @RequestParam(defaultValue = "asc") String direction) {
        return ApiResponse.success("Descansos consultados correctamente", breaks.list(id, page, size, sort, direction));
    }

    @GetMapping("/{id}/breaks/{breakId}")
    @PreAuthorize("hasAuthority('shifts.breaks.detail')")
    public ApiResponse<ShiftBreakResponse> detailBreak(@PathVariable UUID id, @PathVariable UUID breakId) {
        return ApiResponse.success("Descanso consultado correctamente", breaks.detail(id, breakId));
    }

    @PutMapping("/{id}/breaks/{breakId}")
    @PreAuthorize("hasAuthority('shifts.breaks.update')")
    public ApiResponse<ShiftBreakResponse> updateBreak(@PathVariable UUID id, @PathVariable UUID breakId,
            @Valid @RequestBody ShiftBreakRequest request) {
        return ApiResponse.success("Descanso actualizado correctamente", breaks.update(id, breakId, request));
    }

    @DeleteMapping("/{id}/breaks/{breakId}")
    @PreAuthorize("hasAuthority('shifts.breaks.delete')")
    public ApiResponse<Void> deleteBreak(@PathVariable UUID id, @PathVariable UUID breakId) {
        breaks.delete(id, breakId);
        return ApiResponse.success("Descanso eliminado correctamente", null);
    }
}
