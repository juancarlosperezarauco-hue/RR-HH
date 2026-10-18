package com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/employees")
@SecurityRequirement(name = "bearerAuth")
public class EmployeeController {
    private static final Set<String> SORTABLE = Set.of("employeeNumber", "firstName", "lastName",
            "hireDate", "employmentStatus", "createdAt");
    private final CreateEmployeeUseCase create; private final ListEmployeesUseCase list;
    private final GetEmployeeUseCase get; private final UpdateEmployeeUseCase update;
    private final ChangeEmployeeStatusUseCase status;
    private final CreateEmployeeWithAssignmentUseCase onboard;
    private final TransferEmployeeUseCase transfer;
    private final TerminateEmployeeUseCase terminate; private final GetEmployeeTerminationUseCase termination;
    private final ListEmployeePeriodsUseCase periods; private final GetEmployeePeriodUseCase period;
    private final RehireEmployeeUseCase rehire;
    private final ListEmployeeTerminationsUseCase terminations;
    @Autowired
    public EmployeeController(CreateEmployeeUseCase create, ListEmployeesUseCase list, GetEmployeeUseCase get,
                              UpdateEmployeeUseCase update, ChangeEmployeeStatusUseCase status,
                              CreateEmployeeWithAssignmentUseCase onboard, TransferEmployeeUseCase transfer,
                              TerminateEmployeeUseCase terminate, GetEmployeeTerminationUseCase termination,
                              ListEmployeePeriodsUseCase periods, GetEmployeePeriodUseCase period,
                              RehireEmployeeUseCase rehire, ListEmployeeTerminationsUseCase terminations) {
        this.create = create; this.list = list; this.get = get; this.update = update; this.status = status;
        this.onboard = onboard; this.transfer = transfer; this.terminate = terminate; this.termination = termination; this.periods = periods; this.period = period; this.rehire = rehire; this.terminations = terminations;
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('employees.create')")
    public ApiResponse<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        return ApiResponse.success("Empleado registrado correctamente", create.execute(request));
    }
    @PostMapping("/with-assignment")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('employees.create') and hasAuthority('employees.assignments.create')")
    public ApiResponse<EmployeeOnboardingResponse> createWithAssignment(
            @Valid @RequestBody EmployeeOnboardingRequest request) {
        return ApiResponse.success("Empleado, expediente y asignación registrados correctamente", onboard.execute(request));
    }
    @GetMapping
    @PreAuthorize("hasAuthority('employees.list')")
    public ApiResponse<Page<EmployeeSummaryResponse>> list(@RequestParam(required = false) String name,
            @RequestParam(required = false) String employeeNumber,
            @RequestParam(required = false) EmploymentStatus status,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) UUID positionId,
            @ParameterObject @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.success("Empleados consultados correctamente", list.execute(
                new EmployeeFilter(name, employeeNumber, status, departmentId, positionId),
                EmployeePageables.checked(pageable, SORTABLE, "lastName")));
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('employees.detail')")
    public ApiResponse<EmployeeResponse> get(@PathVariable UUID id) {
        return ApiResponse.success("Empleado consultado correctamente", get.execute(id));
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('employees.update')")
    public ApiResponse<EmployeeResponse> update(@PathVariable UUID id, @Valid @RequestBody EmployeeRequest request) {
        return ApiResponse.success("Empleado actualizado correctamente", update.execute(id, request));
    }
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('employees.status.update')")
    public ApiResponse<EmployeeResponse> status(@PathVariable UUID id, @Valid @RequestBody EmployeeStatusRequest request) {
        return ApiResponse.success("Estado laboral actualizado correctamente", status.execute(id, request.status(), request.reason()));
    }

    @PostMapping("/{id}/transfer")
    @PreAuthorize("hasAuthority('employees.transfer')")
    public ApiResponse<EmployeeTransferResponse> transfer(@PathVariable UUID id,
            @Valid @RequestBody TransferEmployeeRequest request) {
        return ApiResponse.success("Transferencia laboral registrada correctamente", transfer.execute(id, request));
    }

    @PostMapping("/{id}/terminate")
    @PreAuthorize("hasAuthority('employees.terminate')")
    public ApiResponse<EmployeeTerminationResponse> terminate(@PathVariable UUID id,
            @Valid @RequestBody TerminateEmployeeRequest request) {
        return ApiResponse.success("Relación laboral finalizada correctamente", terminate.execute(id, request));
    }

    @PostMapping("/{id}/rehire")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('employees.rehire')")
    public ApiResponse<RehireEmployeeResponse> rehire(@PathVariable UUID id,
            @Valid @RequestBody RehireEmployeeRequest request) {
        return ApiResponse.success("Empleado recontratado correctamente", rehire.execute(id, request));
    }

    @GetMapping("/{id}/termination")
    @PreAuthorize("hasAuthority('employees.termination.read')")
    public ApiResponse<EmployeeTerminationHistoryResponse> termination(@PathVariable UUID id) {
        return ApiResponse.success("Desvinculación consultada correctamente", termination.execute(id));
    }

    @GetMapping("/{id}/terminations")
    @PreAuthorize("hasAuthority('employees.termination.read')")
    public ApiResponse<java.util.List<EmployeeTerminationHistoryResponse>> terminations(@PathVariable UUID id) {
        return ApiResponse.success("Desvinculaciones consultadas correctamente", terminations.execute(id));
    }

    @GetMapping("/{id}/employment-periods/{periodId}/termination")
    @PreAuthorize("hasAuthority('employees.termination.read')")
    public ApiResponse<EmployeeTerminationHistoryResponse> terminationForPeriod(@PathVariable UUID id, @PathVariable UUID periodId) {
        return ApiResponse.success("Desvinculación consultada correctamente", terminations.executeForPeriod(id, periodId));
    }

    @GetMapping("/{id}/employment-periods")
    @PreAuthorize("hasAuthority('employees.detail')")
    public ApiResponse<java.util.List<EmploymentPeriodResponse>> periods(@PathVariable UUID id) {
        return ApiResponse.success("Períodos laborales consultados correctamente", periods.execute(id));
    }

    @GetMapping("/{id}/employment-periods/{periodId}")
    @PreAuthorize("hasAuthority('employees.detail')")
    public ApiResponse<EmploymentPeriodResponse> period(@PathVariable UUID id, @PathVariable UUID periodId) {
        return ApiResponse.success("Período laboral consultado correctamente", period.execute(id, periodId));
    }
}
