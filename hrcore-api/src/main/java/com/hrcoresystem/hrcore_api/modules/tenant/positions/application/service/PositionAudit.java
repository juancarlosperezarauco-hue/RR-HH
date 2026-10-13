package com.hrcoresystem.hrcore_api.modules.tenant.positions.application.service;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.model.*;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class PositionAudit {
    private final AuditTrailService audit;
    public PositionAudit(AuditTrailService audit) { this.audit = audit; }

    public void position(String action, Position before, Position after) {
        Position target = after == null ? before : after;
        audit.recordTenantEvent("POSITION_" + action, "POSITION", target.id().toString(),
                Map.of("action", action), before, after);
    }

    public void requirement(String action, PositionRequirement before, PositionRequirement after) {
        PositionRequirement target = after == null ? before : after;
        audit.recordTenantEvent("POSITION_REQUIREMENT_" + action, "POSITION_REQUIREMENT", target.id().toString(),
                Map.of("action", action, "positionId", target.positionId().toString()), before, after);
    }
}
