package com.hrcoresystem.hrcore_api.modules.tenant.departments.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.mapper.DepartmentMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class CreateDepartmentUseCase {
    private final DepartmentRepository departments;
    private final DepartmentPolicy policy;
    private final DepartmentChangeRecorder recorder;
    private final DepartmentMapper mapper;

    public CreateDepartmentUseCase(DepartmentRepository departments, DepartmentPolicy policy,
                                   DepartmentChangeRecorder recorder, DepartmentMapper mapper) {
        this.departments = departments; this.policy = policy; this.recorder = recorder; this.mapper = mapper;
    }

    @Transactional
    public DepartmentResponse execute(CreateDepartmentRequest request) {
        policy.assertOperational();
        policy.lockForWrite();
        String code = policy.required(request.code(), 50, "code");
        String name = policy.required(request.name(), 150, "name");
        policy.assertCodeAvailable(code, null);
        policy.validateReferences(null, request.parentDepartmentId(), request.managerEmployeeId());
        UUID actor = policy.actorId();
        Department saved = departments.save(new Department(null, code, name,
                policy.optional(request.description(), 0, "description"), request.parentDepartmentId(),
                request.managerEmployeeId(), policy.optional(request.costCenterCode(), 50, "costCenterCode"),
                DepartmentStatus.ACTIVE, actor, actor, null, null));
        recorder.record(null, saved, DepartmentHistoryAction.CREATED,
                policy.optional(request.reason(), 0, "reason"), actor);
        return mapper.toResponse(saved);
    }
}
