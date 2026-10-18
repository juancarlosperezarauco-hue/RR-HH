package com.hrcoresystem.hrcore_api.modules.tenant.positions.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.usecase.*;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.model.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.Set;
import java.util.UUID;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/positions")
@SecurityRequirement(name = "bearerAuth")
public class PositionController {
    private static final Set<String> POSITION_SORT = Set.of("code", "name", "status", "organizationalLevel", "createdAt", "updatedAt");
    private static final Set<String> REQUIREMENT_SORT = Set.of("requirementType", "mandatory", "createdAt", "updatedAt");
    private final CreatePositionUseCase createPosition;
    private final ListPositionsUseCase listPositions;
    private final GetPositionUseCase getPosition;
    private final UpdatePositionUseCase updatePosition;
    private final ActivatePositionUseCase activatePosition;
    private final DeactivatePositionUseCase deactivatePosition;
    private final ListPositionRequirementsUseCase listRequirements;
    private final CreatePositionRequirementUseCase createRequirement;
    private final UpdatePositionRequirementUseCase updateRequirement;
    private final DeletePositionRequirementUseCase deleteRequirement;
    private final ListPositionEmployeesUseCase positionEmployees;

    public PositionController(CreatePositionUseCase createPosition, ListPositionsUseCase listPositions,
                              GetPositionUseCase getPosition, UpdatePositionUseCase updatePosition,
                              ActivatePositionUseCase activatePosition, DeactivatePositionUseCase deactivatePosition,
                              ListPositionRequirementsUseCase listRequirements,
                              CreatePositionRequirementUseCase createRequirement,
                              UpdatePositionRequirementUseCase updateRequirement,
                              DeletePositionRequirementUseCase deleteRequirement, ListPositionEmployeesUseCase positionEmployees) {
        this.createPosition = createPosition; this.listPositions = listPositions; this.getPosition = getPosition;
        this.updatePosition = updatePosition; this.activatePosition = activatePosition;
        this.deactivatePosition = deactivatePosition; this.listRequirements = listRequirements;
        this.createRequirement = createRequirement; this.updateRequirement = updateRequirement;
        this.deleteRequirement = deleteRequirement;
        this.positionEmployees = positionEmployees;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('positions.create')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PositionResponse> create(@Valid @RequestBody PositionRequest request) {
        return ApiResponse.success("Cargo creado correctamente", createPosition.execute(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('positions.list')")
    public ApiResponse<Page<PositionResponse>> list(@RequestParam(required = false) PositionStatus status,
            @RequestParam(required = false) String name, @RequestParam(required = false) String code,
            @RequestParam(required = false) OrganizationalLevel organizationalLevel,
            @ParameterObject @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.success("Cargos consultados correctamente", listPositions.execute(
                new PositionFilter(status, name, code, organizationalLevel), checkedPage(pageable, POSITION_SORT, "name")));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('positions.detail')")
    public ApiResponse<PositionResponse> get(@PathVariable UUID id) {
        return ApiResponse.success("Cargo consultado correctamente", getPosition.execute(id));
    }
    @GetMapping("/{id}/employees") @PreAuthorize("hasAuthority('employees.list')")
    public ApiResponse<Page<com.hrcoresystem.hrcore_api.modules.tenant.departments.application.dto.OrganizationalEmployeeSummaryResponse>> employees(@PathVariable UUID id,@RequestParam(required=false) LocalDate date,@ParameterObject @PageableDefault(size=25) Pageable pageable){return ApiResponse.success("Empleados del cargo consultados correctamente",positionEmployees.execute(id,date,checkedPage(pageable,Set.of("fullName","employeeNumber","employeeId"),"fullName")));}

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('positions.update')")
    public ApiResponse<PositionResponse> update(@PathVariable UUID id, @Valid @RequestBody PositionRequest request) {
        return ApiResponse.success("Cargo actualizado correctamente", updatePosition.execute(id, request));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('positions.activate')")
    public ApiResponse<PositionResponse> activate(@PathVariable UUID id) {
        return ApiResponse.success("Cargo activado correctamente", activatePosition.execute(id));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('positions.deactivate')")
    public ApiResponse<PositionResponse> deactivate(@PathVariable UUID id) {
        return ApiResponse.success("Cargo desactivado correctamente", deactivatePosition.execute(id));
    }

    @GetMapping("/{id}/requirements")
    @PreAuthorize("hasAuthority('positions.requirements.list')")
    public ApiResponse<Page<PositionRequirementResponse>> listRequirements(@PathVariable UUID id,
            @ParameterObject @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.success("Requisitos del cargo consultados correctamente",
                listRequirements.execute(id, checkedPage(pageable, REQUIREMENT_SORT, "createdAt")));
    }

    @PostMapping("/{id}/requirements")
    @PreAuthorize("hasAuthority('positions.requirements.create')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PositionRequirementResponse> createRequirement(@PathVariable UUID id,
            @Valid @RequestBody PositionRequirementRequest request) {
        return ApiResponse.success("Requisito registrado correctamente", createRequirement.execute(id, request));
    }

    @PutMapping("/{id}/requirements/{requirementId}")
    @PreAuthorize("hasAuthority('positions.requirements.update')")
    public ApiResponse<PositionRequirementResponse> updateRequirement(@PathVariable UUID id,
            @PathVariable UUID requirementId, @Valid @RequestBody PositionRequirementRequest request) {
        return ApiResponse.success("Requisito actualizado correctamente",
                updateRequirement.execute(id, requirementId, request));
    }

    @DeleteMapping("/{id}/requirements/{requirementId}")
    @PreAuthorize("hasAuthority('positions.requirements.delete')")
    public ApiResponse<Void> deleteRequirement(@PathVariable UUID id, @PathVariable UUID requirementId) {
        deleteRequirement.execute(id, requirementId);
        return ApiResponse.success("Requisito eliminado correctamente", null);
    }

    private Pageable checkedPage(Pageable pageable, Set<String> allowed, String defaultSort) {
        if (pageable.isUnpaged() || pageable.getPageNumber() < 0 || pageable.getPageSize() < 1 || pageable.getPageSize() > 100)
            throw new BusinessException("Page size must be between 1 and 100");
        for (Sort.Order order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) throw new BusinessException("Unsupported sort field: " + order.getProperty());
        }
        Sort sort = pageable.getSort().isUnsorted() ? Sort.by(defaultSort).ascending() : pageable.getSort();
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort.and(Sort.by("id").ascending()));
    }
}
