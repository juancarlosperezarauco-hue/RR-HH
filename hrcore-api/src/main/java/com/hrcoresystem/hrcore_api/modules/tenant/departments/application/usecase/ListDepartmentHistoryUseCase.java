package com.hrcoresystem.hrcore_api.modules.tenant.departments.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.dto.DepartmentHistoryResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.mapper.DepartmentMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.service.DepartmentPolicy;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.repository.DepartmentHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class ListDepartmentHistoryUseCase {
    private final DepartmentHistoryRepository histories;
    private final DepartmentPolicy policy;
    private final DepartmentMapper mapper;

    public ListDepartmentHistoryUseCase(DepartmentHistoryRepository histories, DepartmentPolicy policy,
                                        DepartmentMapper mapper) {
        this.histories = histories; this.policy = policy; this.mapper = mapper;
    }

    public Page<DepartmentHistoryResponse> execute(UUID id, Pageable pageable) {
        policy.assertOperational();
        policy.require(id);
        return histories.findByDepartmentId(id, pageable).map(mapper::toHistoryResponse);
    }
}
