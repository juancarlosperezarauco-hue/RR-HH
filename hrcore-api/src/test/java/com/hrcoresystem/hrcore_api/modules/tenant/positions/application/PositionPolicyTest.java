package com.hrcoresystem.hrcore_api.modules.tenant.positions.application;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.service.PositionPolicy;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.repository.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PositionPolicyTest {
    private final PositionRepository positions = mock(PositionRepository.class);
    private final PositionRequirementRepository requirements = mock(PositionRequirementRepository.class);
    private final PositionPolicy policy = new PositionPolicy(positions, requirements,
            mock(TenantPlanEnforcementService.class), mock(SecurityContextFacade.class));

    @Test
    void validatesCodeNameSalaryAndCurrency() {
        UUID actor = UUID.randomUUID();
        assertThrows(BusinessException.class, () -> policy.normalizedPosition(null,
                request(" ", "Analyst", null, null, null), null, actor));
        assertThrows(BusinessException.class, () -> policy.normalizedPosition(null,
                request("P1", " ", null, null, null), null, actor));
        assertThrows(BusinessException.class, () -> policy.normalizedPosition(null,
                request("P1", "Analyst", new BigDecimal("-1"), null, "USD"), null, actor));
        assertThrows(BusinessException.class, () -> policy.normalizedPosition(null,
                request("P1", "Analyst", new BigDecimal("2"), BigDecimal.ONE, "USD"), null, actor));
        assertThrows(BusinessException.class, () -> policy.normalizedPosition(null,
                request("P1", "Analyst", new BigDecimal("1.00001"), null, "USD"), null, actor));
        assertThrows(BusinessException.class, () -> policy.normalizedPosition(null,
                request("P1", "Analyst", null, null, "usd"), null, actor));
        when(positions.existsByCodeOtherThan("P1", null)).thenReturn(true);
        assertThrows(BusinessException.class, () -> policy.normalizedPosition(null,
                request("P1", "Analyst", null, null, null), null, actor));
    }

    @Test
    void createsActivePositionWithSchemaDefaultLevel() {
        Position p = policy.normalizedPosition(null, request(" P1 ", " Analyst ", BigDecimal.ZERO, null, null),
                null, UUID.randomUUID());
        assertEquals("P1", p.code());
        assertEquals("Analyst", p.name());
        assertEquals(PositionStatus.ACTIVE, p.status());
        assertEquals(OrganizationalLevel.OPERATIONAL, p.organizationalLevel());
        assertEquals(new BigDecimal("0.0000"), p.salaryMin());
    }

    @Test
    void validatesRequirementAndOwnership() {
        UUID positionId = UUID.randomUUID();
        UUID requirementId = UUID.randomUUID();
        assertThrows(BusinessException.class, () -> policy.normalizedRequirement(positionId,
                new PositionRequirementRequest(RequirementType.SKILL, "Skill", true, -1), null));
        assertThrows(BusinessException.class, () -> policy.normalizedRequirement(positionId,
                new PositionRequirementRequest(RequirementType.SKILL, " ", true, null), null));
        assertThrows(BusinessException.class, () -> policy.normalizedRequirement(positionId,
                new PositionRequirementRequest(RequirementType.SKILL, "Skill", null, null), null));
        assertThrows(ResourceNotFoundException.class, () -> policy.requireRequirement(positionId, requirementId));
        verify(requirements).findByIdAndPositionId(requirementId, positionId);
        PositionRequirement owned = new PositionRequirement(requirementId, positionId, RequirementType.SKILL,
                "Skill", true, null, null, null);
        when(requirements.findByIdAndPositionId(requirementId, positionId)).thenReturn(Optional.of(owned));
        assertEquals(owned, policy.requireRequirement(positionId, requirementId));
    }

    private PositionRequest request(String code, String name, BigDecimal min, BigDecimal max, String currency) {
        return new PositionRequest(code, name, null, null, null, min, max, currency);
    }
}
