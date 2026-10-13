package com.hrcoresystem.hrcore_api.modules.tenant.departments.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.dto.DepartmentResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.mapper.DepartmentMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class DeactivateDepartmentUseCase {
    private final DepartmentRepository departments;
    private final DepartmentPolicy policy;
    private final DepartmentChangeRecorder recorder;
    private final DepartmentMapper mapper;

    public DeactivateDepartmentUseCase(DepartmentRepository departments, DepartmentPolicy policy,
                                       DepartmentChangeRecorder recorder, DepartmentMapper mapper) {
        this.departments = departments; this.policy = policy; this.recorder = recorder; this.mapper = mapper;
    }

    @Transactional
    public DepartmentResponse execute(UUID id, String reason) {
        policy.assertOperational();
        policy.lockForWrite();
        Department before = policy.require(id);
        if (before.status() == DepartmentStatus.INACTIVE) return mapper.toResponse(before);
        String requiredReason = policy.requiredReason(reason);
        policy.validateDeactivation(id);
        UUID actor = policy.actorId();
        Department saved = departments.save(new Department(id, before.code(), before.name(), before.description(),
                before.parentDepartmentId(), before.managerEmployeeId(), before.costCenterCode(),
                DepartmentStatus.INACTIVE, before.createdBy(), actor, before.createdAt(), before.updatedAt()));
        recorder.record(before, saved, DepartmentHistoryAction.DEACTIVATED, requiredReason, actor);
        return mapper.toResponse(saved);
    }
}
