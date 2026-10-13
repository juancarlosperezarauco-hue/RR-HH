package com.hrcoresystem.hrcore_api.modules.tenant.departments.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.mapper.DepartmentMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
import java.util.UUID;

@Service
public class UpdateDepartmentUseCase {
    private final DepartmentRepository departments;
    private final DepartmentPolicy policy;
    private final DepartmentChangeRecorder recorder;
    private final DepartmentMapper mapper;

    public UpdateDepartmentUseCase(DepartmentRepository departments, DepartmentPolicy policy,
                                   DepartmentChangeRecorder recorder, DepartmentMapper mapper) {
        this.departments = departments; this.policy = policy; this.recorder = recorder; this.mapper = mapper;
    }

    @Transactional
    public DepartmentResponse execute(UUID id, UpdateDepartmentRequest request) {
        policy.assertOperational();
        policy.lockForWrite();
        Department before = policy.require(id);
        String code = policy.required(request.code(), 50, "code");
        String name = policy.required(request.name(), 150, "name");
        policy.assertCodeAvailable(code, id);
        policy.validateReferences(id, request.parentDepartmentId(), request.managerEmployeeId());
        boolean sensitive = !Objects.equals(before.parentDepartmentId(), request.parentDepartmentId())
                || !Objects.equals(before.managerEmployeeId(), request.managerEmployeeId());
        String reason = sensitive ? policy.requiredReason(request.reason())
                : policy.optional(request.reason(), 0, "reason");
        Department after = new Department(id, code, name,
                policy.optional(request.description(), 0, "description"), request.parentDepartmentId(),
                request.managerEmployeeId(), policy.optional(request.costCenterCode(), 50, "costCenterCode"),
                before.status(), before.createdBy(), before.updatedBy(), before.createdAt(), before.updatedAt());
        if (!recorder.changed(before, after)) return mapper.toResponse(before);
        UUID actor = policy.actorId();
        after = new Department(id, after.code(), after.name(), after.description(), after.parentDepartmentId(),
                after.managerEmployeeId(), after.costCenterCode(), after.status(), before.createdBy(), actor,
                before.createdAt(), before.updatedAt());
        Department saved = departments.save(after);
        recorder.record(before, saved, recorder.updateAction(before, saved), reason, actor);
        return mapper.toResponse(saved);
    }
}
