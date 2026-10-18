package com.hrcoresystem.hrcore_api.modules.tenant.leaves.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessConflictException;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.domain.model.LeaveModels.LeaveRequest;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.domain.model.LeaveModels.RequestStatus;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.domain.repository.LeaveStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class EmployeeLeaveTerminationService {
    private static final int PAGE_SIZE = 100;
    private static final String TERMINATION_REASON = "Solicitud cancelada por desvinculación laboral";
    private final LeaveStore store;
    private final LeaveService leaves;

    public EmployeeLeaveTerminationService(LeaveStore store, LeaveService leaves) {
        this.store = store;
        this.leaves = leaves;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void reconcile(UUID employeeId, UUID employmentPeriodId, LocalDate terminationDate) {
        Objects.requireNonNull(employeeId);
        Objects.requireNonNull(employmentPeriodId);
        Objects.requireNonNull(terminationDate);
        forEachCandidate(employeeId, employmentPeriodId, terminationDate, request -> {
            if (request.status() == RequestStatus.APPROVED && !request.startDate().isAfter(terminationDate))
                throw new BusinessConflictException("El empleado tiene un permiso aprobado en curso. Debe regularizarse antes de finalizar la relación laboral. Solicitud: " + request.id());
        });
        forEachCandidate(employeeId, employmentPeriodId, terminationDate, request -> {
            if (request.status() == RequestStatus.DRAFT || request.status() == RequestStatus.PENDING
                    || request.status() == RequestStatus.APPROVED)
                leaves.cancel(request.id(), TERMINATION_REASON);
        });
    }

    private void forEachCandidate(UUID employeeId, UUID employmentPeriodId, LocalDate terminationDate,
            Consumer<LeaveRequest> action) {
        for (int pageNumber = 0; ; pageNumber++) {
            Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id"));
            Page<LeaveRequest> page = store.requests(employeeId, null, null, terminationDate, null, pageable);
            for (LeaveRequest request : page.getContent()) {
                if (employmentPeriodId.equals(request.employmentPeriodId())) action.accept(request);
            }
            if (!page.hasNext()) break;
        }
    }
}
