package com.hrcoresystem.hrcore_api.modules.tenant.contracts.application.service;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.repository.EmploymentContractRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.repository.ContractHistoryRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.NotificationPublishRequest;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.port.NotificationPublisherPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Map;
@Service
public class ContractExpirationService {
    private final EmploymentContractRepository contracts; private final ContractHistoryRepository history;
    private final AuditTrailService audit; private final EmployeeRepository employees; private final NotificationPublisherPort notifications;
    public ContractExpirationService(EmploymentContractRepository contracts, ContractHistoryRepository history,
            AuditTrailService audit, EmployeeRepository employees, NotificationPublisherPort notifications) { this.contracts = contracts; this.history = history; this.audit = audit; this.employees = employees; this.notifications = notifications; }
    @Transactional
    public int expireDue(LocalDate today) {
        int count = 0;
        for (EmploymentContract candidate : contracts.findExpiredBatchForUpdate(today)) {
            EmploymentContract locked = contracts.findByIdForUpdate(candidate.id()).orElse(null);
            if (locked == null || locked.status() != ContractStatus.ACTIVE || locked.endDate() == null || !locked.endDate().isBefore(today)) continue;
            EmploymentContract expired = new EmploymentContract(locked.id(), locked.employeeId(), locked.employeeAssignmentId(), locked.contractNumber(),
                    locked.contractType(), ContractStatus.EXPIRED, locked.contractForm(), locked.workSchedule(), locked.workModality(),
                    locked.startDate(), locked.endDate(), locked.signedAt(), locked.baseSalary(), locked.currency(), locked.paymentFrequency(),
                    locked.weeklyHours(), locked.probationEndDate(), locked.terminationDate(), locked.terminationReason(), locked.terms(),
                    locked.renewedFromContractId(), locked.createdBy(), locked.updatedBy(), locked.createdAt(), locked.updatedAt(), locked.employmentPeriodId());
            contracts.save(expired);
            history.save(new ContractHistory(null, locked.id(), ContractHistoryAction.EXPIRED, ContractStatus.ACTIVE,
                    ContractStatus.EXPIRED, Map.of(), "Contract end date reached", null, null));
            audit.recordTenantEvent("CONTRACT_EXPIRED", "EMPLOYMENT_CONTRACT", locked.id().toString(), Map.of("contractNumber", locked.contractNumber()));
            try {
                employees.findById(locked.employeeId()).map(e -> e.tenantUserId()).ifPresent(userId ->
                    notifications.publish(new NotificationPublishRequest(userId, NotificationType.CONTRACT_EXPIRED,
                        NotificationCategory.SYSTEM, NotificationPriority.HIGH, "Contrato vencido",
                        "Tu contrato laboral " + (locked.contractNumber() == null ? "" : locked.contractNumber()) + " ha vencido.",
                        null, null, "/dashboard/contracts?id=" + locked.id(), null)));
            } catch (Exception ignored) { /* Las notificaciones nunca deben bloquear la expiración. */ }
            count++;
        }
        return count;
    }
}
