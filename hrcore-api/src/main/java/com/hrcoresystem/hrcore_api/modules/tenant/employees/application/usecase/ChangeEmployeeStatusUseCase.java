package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.EmployeeResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.mapper.EmployeeMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.NotificationPublishRequest;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationCategory;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationPriority;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationType;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.port.NotificationPublisherPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class ChangeEmployeeStatusUseCase {
    private final EmployeeRepository employees; private final EmployeePolicy policy;
    private final EmployeeAudit audit; private final EmployeeMapper mapper;
    private final NotificationPublisherPort notificationPublisher;
    @Autowired
    public ChangeEmployeeStatusUseCase(EmployeeRepository employees, EmployeePolicy policy,
                                       EmployeeAudit audit, EmployeeMapper mapper,
                                       NotificationPublisherPort notificationPublisher) {
        this.employees = employees; this.policy = policy; this.audit = audit; this.mapper = mapper;
        this.notificationPublisher = notificationPublisher;
    }
    public ChangeEmployeeStatusUseCase(EmployeeRepository employees, EmployeePolicy policy,
                                       EmployeeAudit audit, EmployeeMapper mapper) {
        this(employees, policy, audit, mapper, null);
    }
    @Transactional
    public EmployeeResponse execute(UUID id, EmploymentStatus status) {
        return execute(id, status, null);
    }
    @Transactional
    public EmployeeResponse execute(UUID id, EmploymentStatus status, String reason) {
        policy.assertOperational();
        Employee before = policy.requireForUpdate(id);
        policy.assertNotTerminated(before);
        if (status == null || status == EmploymentStatus.TERMINATED)
            throw new BusinessException("Unsupported employment status");
        if (before.employmentStatus() == status) return mapper.toResponse(before);
        Employee saved = employees.save(policy.changedStatus(before, status, reason, policy.actorId()));
        audit.record("EMPLOYEE", "STATUS_UPDATED", id, List.of("employmentStatus"));
        publishStatusNotification(saved);
        return mapper.toResponse(saved);
    }

    private void publishStatusNotification(Employee employee) {
        if (notificationPublisher == null || employee.tenantUserId() == null) return;
        NotificationType type = switch (employee.employmentStatus()) {
            case SUSPENDED -> NotificationType.ACCOUNT_FROZEN;
            case ACTIVE -> NotificationType.ACCOUNT_ACTIVATED;
            default -> NotificationType.SYSTEM_NOTICE;
        };
        String title = switch (employee.employmentStatus()) {
            case SUSPENDED -> "Relación laboral suspendida";
            case ON_LEAVE -> "Empleado en ausencia";
            case INACTIVE -> "Relación laboral inactiva";
            case ACTIVE -> "Relación laboral activa";
            default -> "Estado laboral actualizado";
        };
        String body = "El estado de tu relación laboral fue actualizado a " + employmentStatusLabel(employee.employmentStatus()) + ".";
        try {
            notificationPublisher.publish(new NotificationPublishRequest(
                    employee.tenantUserId(), type, NotificationCategory.SYSTEM,
                    NotificationPriority.NORMAL, title, body, null, null,
                    null, null));
        } catch (Exception ignored) {
            // La notificación es secundaria y no debe revertir el cambio laboral.
        }
    }

    private String employmentStatusLabel(EmploymentStatus status) {
        return switch (status) {
            case ACTIVE -> "Activa";
            case ON_LEAVE -> "En ausencia";
            case SUSPENDED -> "Suspendida";
            case TERMINATED -> "Finalizada";
            case INACTIVE -> "Inactiva";
        };
    }
}
