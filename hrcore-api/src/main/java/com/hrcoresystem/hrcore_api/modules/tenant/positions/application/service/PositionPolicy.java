package com.hrcoresystem.hrcore_api.modules.tenant.positions.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.repository.*;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

@Component
public class PositionPolicy {
    private final PositionRepository positions;
    private final PositionRequirementRepository requirements;
    private final TenantPlanEnforcementService plans;
    private final SecurityContextFacade security;

    public PositionPolicy(PositionRepository positions, PositionRequirementRepository requirements,
                          TenantPlanEnforcementService plans, SecurityContextFacade security) {
        this.positions = positions; this.requirements = requirements; this.plans = plans; this.security = security;
    }

    public void assertOperational() { plans.assertCanOperate(); }

    public Position requirePosition(UUID id) {
        return positions.findById(id).orElseThrow(() -> new ResourceNotFoundException("Position not found: " + id));
    }

    public Position requirePositionForUpdate(UUID id) {
        return positions.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Position not found: " + id));
    }

    public PositionRequirement requireRequirement(UUID positionId, UUID requirementId) {
        return requirements.findByIdAndPositionId(requirementId, positionId)
                .orElseThrow(() -> new ResourceNotFoundException("Position requirement not found: " + requirementId));
    }

    public UUID actorId() {
        try { return UUID.fromString(security.getCurrentSubject()); }
        catch (RuntimeException ex) { throw new BusinessException("Authenticated tenant user is required"); }
    }

    public Position normalizedPosition(UUID id, PositionRequest request, Position before, UUID actor) {
        String code = required(request.code(), 50, "code");
        String name = required(request.name(), 150, "name");
        if (positions.existsByCodeOtherThan(code, id)) throw new BusinessException("Position code already exists");
        BigDecimal min = salary(request.salaryMin(), "salaryMin");
        BigDecimal max = salary(request.salaryMax(), "salaryMax");
        if (min != null && max != null && min.compareTo(max) > 0)
            throw new BusinessException("salaryMin must not exceed salaryMax");
        String currency = optional(request.currency());
        if (currency != null && !currency.matches("[A-Z]{3}"))
            throw new BusinessException("currency must contain three uppercase letters");
        return new Position(id, code, name, optional(request.description()), optional(request.responsibilities()),
                request.organizationalLevel() == null ? OrganizationalLevel.OPERATIONAL : request.organizationalLevel(),
                min, max, currency, before == null ? PositionStatus.ACTIVE : before.status(),
                before == null ? actor : before.createdBy(), actor,
                before == null ? null : before.createdAt(), before == null ? null : before.updatedAt());
    }

    public PositionRequirement normalizedRequirement(UUID positionId, PositionRequirementRequest request,
                                                      PositionRequirement before) {
        if (request.requirementType() == null) throw new BusinessException("requirementType is required");
        if (request.mandatory() == null) throw new BusinessException("mandatory is required");
        if (request.minimumYearsExperience() != null && request.minimumYearsExperience() < 0)
            throw new BusinessException("minimumYearsExperience must be nonnegative");
        return new PositionRequirement(before == null ? null : before.id(), positionId, request.requirementType(),
                required(request.description(), 0, "description"), request.mandatory(),
                request.minimumYearsExperience(), before == null ? null : before.createdAt(),
                before == null ? null : before.updatedAt());
    }

    public boolean sameContent(Position a, Position b) {
        return Objects.equals(a.code(), b.code()) && Objects.equals(a.name(), b.name())
                && Objects.equals(a.description(), b.description())
                && Objects.equals(a.responsibilities(), b.responsibilities())
                && a.organizationalLevel() == b.organizationalLevel()
                && Objects.equals(a.salaryMin(), b.salaryMin()) && Objects.equals(a.salaryMax(), b.salaryMax())
                && Objects.equals(a.currency(), b.currency());
    }

    public boolean sameContent(PositionRequirement a, PositionRequirement b) {
        return a.requirementType() == b.requirementType()
                && Objects.equals(a.description(), b.description())
                && a.mandatory() == b.mandatory()
                && Objects.equals(a.minimumYearsExperience(), b.minimumYearsExperience());
    }

    private BigDecimal salary(BigDecimal value, String field) {
        if (value == null) return null;
        if (value.signum() < 0) throw new BusinessException(field + " must be nonnegative");
        try {
            BigDecimal scaled = value.setScale(4, RoundingMode.UNNECESSARY);
            if (scaled.precision() > 19) throw new BusinessException(field + " exceeds NUMERIC(19,4)");
            return scaled;
        } catch (ArithmeticException ex) {
            throw new BusinessException(field + " exceeds four decimal places");
        }
    }

    private String required(String value, int max, String field) {
        if (value == null || value.trim().isEmpty()) throw new BusinessException(field + " is required");
        String normalized = value.trim();
        if (max > 0 && normalized.length() > max) throw new BusinessException(field + " exceeds " + max + " characters");
        return normalized;
    }

    private String optional(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
