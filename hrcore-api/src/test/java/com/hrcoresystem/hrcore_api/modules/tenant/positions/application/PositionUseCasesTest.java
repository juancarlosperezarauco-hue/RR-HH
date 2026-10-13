package com.hrcoresystem.hrcore_api.modules.tenant.positions.application;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.mapper.PositionMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.usecase.*;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.UUID;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PositionUseCasesTest {
    private final PositionRepository positions = mock(PositionRepository.class);
    private final PositionRequirementRepository requirements = mock(PositionRequirementRepository.class);
    private final PositionPolicy policy = mock(PositionPolicy.class);
    private final PositionAudit audit = mock(PositionAudit.class);
    private final PositionMapper mapper = new PositionMapper();

    @Test
    void createsPositionAndAuditsIt() {
        PositionRequest request = new PositionRequest("P1", "Analyst", null, null, null, null, null, null);
        Position created = position(PositionStatus.ACTIVE);
        when(policy.normalizedPosition(eq(null), eq(request), eq(null), any())).thenReturn(created);
        when(policy.actorId()).thenReturn(UUID.randomUUID());
        when(positions.save(created)).thenReturn(created);
        PositionResponse response = new CreatePositionUseCase(positions, policy, audit, mapper).execute(request);
        assertEquals(created.id(), response.id());
        verify(audit).position("CREATED", null, created);
        verify(policy).assertOperational();
    }

    @Test
    void unchangedUpdateDoesNotWriteOrAudit() {
        Position before = position(PositionStatus.ACTIVE);
        PositionRequest request = new PositionRequest("P1", "Analyst", null, null, null, null, null, null);
        when(policy.requirePositionForUpdate(before.id())).thenReturn(before);
        when(policy.actorId()).thenReturn(UUID.randomUUID());
        when(policy.normalizedPosition(eq(before.id()), eq(request), eq(before), any())).thenReturn(before);
        when(policy.sameContent(before, before)).thenReturn(true);
        assertEquals(before.id(), new UpdatePositionUseCase(positions, policy, audit, mapper)
                .execute(before.id(), request).id());
        verify(positions, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void effectiveUpdateWritesAndAuditsOnce() {
        Position before = position(PositionStatus.ACTIVE);
        Position after = new Position(before.id(), before.code(), "Senior Analyst", null, null,
                before.organizationalLevel(), null, null, null, before.status(), before.createdBy(),
                UUID.randomUUID(), before.createdAt(), before.updatedAt());
        PositionRequest request = new PositionRequest("P1", "Senior Analyst", null, null, null, null, null, null);
        when(policy.requirePositionForUpdate(before.id())).thenReturn(before);
        when(policy.actorId()).thenReturn(UUID.randomUUID());
        when(policy.normalizedPosition(eq(before.id()), eq(request), eq(before), any())).thenReturn(after);
        when(positions.save(after)).thenReturn(after);
        assertEquals("Senior Analyst", new UpdatePositionUseCase(positions, policy, audit, mapper)
                .execute(before.id(), request).name());
        verify(audit).position("UPDATED", before, after);
    }

    @Test
    void deactivationBlocksCurrentAssignments() {
        Position before = position(PositionStatus.ACTIVE);
        when(policy.requirePositionForUpdate(before.id())).thenReturn(before);
        when(positions.hasCurrentAssignments(before.id())).thenReturn(true);
        assertThrows(BusinessException.class,
                () -> new DeactivatePositionUseCase(positions, policy, audit, mapper).execute(before.id()));
        verify(positions, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void repeatedActivationIsNoOp() {
        Position before = position(PositionStatus.ACTIVE);
        when(policy.requirePositionForUpdate(before.id())).thenReturn(before);
        new ActivatePositionUseCase(positions, policy, audit, mapper).execute(before.id());
        verify(positions, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void deactivationWithoutAssignmentsChangesStatusAndAudits() {
        Position before = position(PositionStatus.ACTIVE);
        when(policy.requirePositionForUpdate(before.id())).thenReturn(before);
        when(policy.actorId()).thenReturn(UUID.randomUUID());
        when(positions.save(any(Position.class))).thenAnswer(call -> call.getArgument(0));
        PositionResponse response = new DeactivatePositionUseCase(positions, policy, audit, mapper).execute(before.id());
        assertEquals(PositionStatus.INACTIVE, response.status());
        verify(audit).position(eq("DEACTIVATED"), eq(before), any(Position.class));
    }

    @Test
    void listPassesFiltersAndPageableToRepository() {
        PositionFilter filter = new PositionFilter(PositionStatus.ACTIVE, "Analyst", "P1", OrganizationalLevel.OPERATIONAL);
        PageRequest page = PageRequest.of(2, 10);
        Position item = position(PositionStatus.ACTIVE);
        when(positions.findAll(filter, page)).thenReturn(new PageImpl<>(List.of(item), page, 21));
        var result = new ListPositionsUseCase(positions, policy, mapper).execute(filter, page);
        assertEquals(21, result.getTotalElements());
        assertEquals(2, result.getNumber());
        verify(positions).findAll(filter, page);
    }

    @Test
    void requirementUpdateNoOpAndDeleteAudits() {
        UUID positionId = UUID.randomUUID();
        PositionRequirement before = new PositionRequirement(UUID.randomUUID(), positionId,
                RequirementType.SKILL, "Java", true, null, null, null);
        PositionRequirementRequest request = new PositionRequirementRequest(RequirementType.SKILL, "Java", true, null);
        when(policy.requireRequirement(positionId, before.id())).thenReturn(before);
        when(policy.normalizedRequirement(positionId, request, before)).thenReturn(before);
        when(policy.sameContent(before, before)).thenReturn(true);
        new UpdatePositionRequirementUseCase(requirements, policy, audit, mapper)
                .execute(positionId, before.id(), request);
        verify(requirements, never()).save(any());
        verifyNoInteractions(audit);
        new DeletePositionRequirementUseCase(requirements, policy, audit).execute(positionId, before.id());
        verify(requirements).delete(before);
        verify(audit).requirement("DELETED", before, null);
    }

    @Test
    void requirementCreationIsScopedToParentAndAudited() {
        UUID positionId = UUID.randomUUID();
        PositionRequirementRequest request = new PositionRequirementRequest(RequirementType.SKILL, "Java", true, 2);
        PositionRequirement created = new PositionRequirement(UUID.randomUUID(), positionId,
                RequirementType.SKILL, "Java", true, 2, null, null);
        when(policy.normalizedRequirement(positionId, request, null)).thenReturn(created);
        when(requirements.save(created)).thenReturn(created);
        var response = new CreatePositionRequirementUseCase(requirements, policy, audit, mapper)
                .execute(positionId, request);
        assertEquals(positionId, response.positionId());
        verify(policy).requirePositionForUpdate(positionId);
        verify(audit).requirement("CREATED", null, created);
    }

    private Position position(PositionStatus status) {
        return new Position(UUID.randomUUID(), "P1", "Analyst", null, null, OrganizationalLevel.OPERATIONAL,
                null, null, null, status, UUID.randomUUID(), null, null, null);
    }
}
