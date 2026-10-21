package com.hrcoresystem.hrcore_api.modules.tenant.contracts.application.service;

import com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.Employee;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.*;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.repository.DepartmentRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.repository.PositionRepository;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository.TenantSettingRepository;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContractPdfCompatibilityTest {
    @Test void existingContractGeneratorStillProducesReadablePdfAfterLibraryUpgrade() throws Exception {
        var policy = mock(ContractPolicy.class); var employees = mock(EmployeeRepository.class);
        var contract = mock(EmploymentContract.class); var employee = mock(Employee.class);
        UUID id = UUID.randomUUID(), employeeId = UUID.randomUUID();
        when(policy.require(id)).thenReturn(contract); when(contract.employeeId()).thenReturn(employeeId);
        when(contract.contractNumber()).thenReturn("SYNTHETIC-001"); when(contract.status()).thenReturn(ContractStatus.DRAFT);
        when(contract.contractType()).thenReturn(ContractType.INDEFINITE);
        when(contract.paymentFrequency()).thenReturn(PaymentFrequency.MONTHLY);
        when(contract.startDate()).thenReturn(LocalDate.of(2026, 1, 1));
        when(contract.baseSalary()).thenReturn(new BigDecimal("3000")); when(contract.currency()).thenReturn("BOB");
        when(employees.findById(employeeId)).thenReturn(Optional.of(employee));
        when(employee.firstName()).thenReturn("Áda"); when(employee.lastName()).thenReturn("Ficticia");
        var service = new ContractPdfService(policy, employees, mock(EmployeeAssignmentRepository.class),
                mock(DepartmentRepository.class), mock(PositionRepository.class), mock(TenantSettingRepository.class));
        var result = service.generate(id);
        try (var reader = new PdfReader(result.content())) {
            String text = new PdfTextExtractor(reader).getTextFromPage(1);
            assertTrue(text.contains("CONTRATO INDIVIDUAL DE TRABAJO")); assertTrue(text.contains("Áda Ficticia"));
        }
    }
}
