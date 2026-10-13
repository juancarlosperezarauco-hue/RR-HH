package com.hrcoresystem.hrcore_api.modules.tenant.contracts.application.mapper;

import com.hrcoresystem.hrcore_api.modules.tenant.contracts.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.model.*;
import org.springframework.stereotype.Component;

@Component
public class ContractMapper {
    public ContractResponse toResponse(EmploymentContract c) {
        return new ContractResponse(c.id(), c.employeeId(), c.employeeAssignmentId(), c.contractNumber(),
                c.contractType(), c.status(), c.contractForm(), c.workSchedule(), c.workModality(),
                c.startDate(), c.endDate(), c.signedAt(), c.baseSalary(), c.currency(),
                c.paymentFrequency(), c.weeklyHours(), c.probationEndDate(), c.terminationDate(),
                c.terminationReason(), c.terms(), c.renewedFromContractId(), c.createdAt(), c.updatedAt());
    }
    public ContractHistoryResponse toResponse(ContractHistory h) {
        return new ContractHistoryResponse(h.id(), h.contractId(), h.action(), h.previousStatus(),
                h.newStatus(), h.details(), h.reason(), h.performedBy(), h.createdAt());
    }
}
