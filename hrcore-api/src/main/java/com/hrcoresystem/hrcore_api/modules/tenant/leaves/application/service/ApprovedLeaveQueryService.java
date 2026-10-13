package com.hrcoresystem.hrcore_api.modules.tenant.leaves.application.service;

import com.hrcoresystem.hrcore_api.modules.tenant.leaves.domain.repository.LeaveStore;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovedLeaveQueryService implements ApprovedLeaveLookup {
    private final LeaveStore store;

    public ApprovedLeaveQueryService(LeaveStore store) { this.store = store; }

    @Override @Transactional(readOnly = true)
    public List<ApprovedPeriod> onDate(UUID employeeId, LocalDate date) {
        if (employeeId == null || date == null) throw new IllegalArgumentException("employeeId and date are required");
        return store.approvedOn(employeeId, date).stream()
                .map(r -> new ApprovedPeriod(r.id(), r.leaveTypeId(), r.startDate(), r.endDate(),
                        r.startTime(), r.endTime(), r.requestedDays()))
                .toList();
    }
}
