package com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository;

import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.WorkShift;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.WorkShiftFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface WorkShiftRepository {
    WorkShift save(WorkShift shift);
    Optional<WorkShift> findById(UUID id);
    Optional<WorkShift> lockById(UUID id);
    boolean existsByCodeOtherThan(String code, UUID id);
    Page<WorkShift> findAll(WorkShiftFilter filter, Pageable pageable);
}
