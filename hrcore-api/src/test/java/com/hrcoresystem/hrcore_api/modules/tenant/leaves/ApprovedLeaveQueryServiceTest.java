package com.hrcoresystem.hrcore_api.modules.tenant.leaves;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hrcoresystem.hrcore_api.modules.tenant.leaves.application.service.ApprovedLeaveQueryService;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.domain.model.LeaveModels.*;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.domain.repository.LeaveStore;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ApprovedLeaveQueryServiceTest {
    @Test void exposesOnlyTheApprovedAbsencePeriodNeededByAttendance() {
        LeaveStore store = mock(LeaveStore.class);
        UUID employee = UUID.randomUUID(), type = UUID.randomUUID(), id = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 11, 2);
        when(store.approvedOn(employee, date)).thenReturn(List.of(new LeaveRequest(id, employee,
                type, date, date, LocalTime.of(9, 0), LocalTime.of(11, 0),
                new BigDecimal("0.25"), "Private medical reason", RequestStatus.APPROVED,
                UUID.randomUUID(), null, UUID.randomUUID(), null, null, null, null)));
        var result = new ApprovedLeaveQueryService(store).onDate(employee, date);
        assertEquals(1, result.size());
        assertEquals(new BigDecimal("0.25"), result.getFirst().requestedDays());
        assertEquals(LocalTime.of(9, 0), result.getFirst().startTime());
        assertTrue(Arrays.stream(result.getFirst().getClass().getRecordComponents())
                .noneMatch(component -> component.getName().equals("reason")));
    }
}
