package com.hrcoresystem.hrcore_api.modules.tenant.positions.application.mapper;

import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.model.*;
import org.springframework.stereotype.Component;

@Component
public class PositionMapper {
    public PositionResponse toResponse(Position p) {
        return new PositionResponse(p.id(), p.code(), p.name(), p.description(), p.responsibilities(),
                p.organizationalLevel(), p.salaryMin(), p.salaryMax(), p.currency(), p.status(),
                p.createdAt(), p.updatedAt());
    }

    public PositionRequirementResponse toResponse(PositionRequirement r) {
        return new PositionRequirementResponse(r.id(), r.positionId(), r.requirementType(),
                r.description(), r.mandatory(), r.minimumYearsExperience(), r.createdAt(), r.updatedAt());
    }
}
