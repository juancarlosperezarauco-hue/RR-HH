package com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.api.EmployeeController;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmployeePageablesTest {
    @Test void rejectsUnsupportedSortAndExcessivePageSize() {
        EmployeeController controller = new EmployeeController(mock(CreateEmployeeUseCase.class),
                mock(ListEmployeesUseCase.class), mock(GetEmployeeUseCase.class),
                mock(UpdateEmployeeUseCase.class), mock(ChangeEmployeeStatusUseCase.class),
                mock(CreateEmployeeWithAssignmentUseCase.class));
        assertThrows(BusinessException.class, () -> controller.list(null, null, null, null, null,
                PageRequest.of(0, 10, Sort.by("documentNumber"))));
        assertThrows(BusinessException.class, () -> controller.list(null, null, null, null, null,
                PageRequest.of(0, 101)));
    }
}
