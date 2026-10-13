package com.hrcoresystem.hrcore_api.modules.tenant.employees.application;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.EmployeeEmergencyContactRequest;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.*;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContactPolicyTest {
    private final EmployeeEmergencyContactRepository contacts = mock(EmployeeEmergencyContactRepository.class);
    private final EmployeePolicy employees = new EmployeePolicy(mock(EmployeeRepository.class),
            mock(EmployeeAssignmentRepository.class), mock(TenantUserRepository.class),
            mock(TenantPlanEnforcementService.class), mock(SecurityContextFacade.class));
    private final ContactPolicy policy = new ContactPolicy(contacts, employees);

    @Test void rejectsSecondPrimaryAndInvalidPhone() {
        UUID employeeId = UUID.randomUUID();
        when(contacts.existsOtherPrimary(employeeId, null)).thenReturn(true);
        assertThrows(BusinessException.class, () -> policy.normalized(employeeId,
                new EmployeeEmergencyContactRequest("Contact", null, "+59170000000", null, null, true), null));
        reset(contacts);
        assertThrows(BusinessException.class, () -> policy.normalized(employeeId,
                new EmployeeEmergencyContactRequest("Contact", null, "invalid!", null, null, false), null));
    }
    @Test void requiresContactWithinTheEmployee() {
        UUID employeeId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        assertThrows(ResourceNotFoundException.class, () -> policy.require(employeeId, contactId));
        verify(contacts).findByIdAndEmployeeId(contactId, employeeId);
    }
}
