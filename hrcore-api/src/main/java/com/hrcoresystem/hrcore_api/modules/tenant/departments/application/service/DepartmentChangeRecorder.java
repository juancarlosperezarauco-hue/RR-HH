package com.hrcoresystem.hrcore_api.modules.tenant.departments.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.repository.DepartmentHistoryRepository;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Component
public class DepartmentChangeRecorder {
    private final DepartmentHistoryRepository histories;
    private final AuditTrailService audit;
    private final ObjectMapper json;

    public DepartmentChangeRecorder(DepartmentHistoryRepository histories, AuditTrailService audit, ObjectMapper json) {
        this.histories = histories;
        this.audit = audit;
        this.json = json;
    }

    public DepartmentHistoryAction updateAction(Department before, Department after) {
        if (!Objects.equals(before.parentDepartmentId(), after.parentDepartmentId())) return DepartmentHistoryAction.REORGANIZED;
        if (!Objects.equals(before.managerEmployeeId(), after.managerEmployeeId())) return DepartmentHistoryAction.MANAGER_CHANGED;
        return DepartmentHistoryAction.UPDATED;
    }

    public boolean changed(Department before, Department after) { return !snapshot(before).equals(snapshot(after)); }

    public void record(Department before, Department after, DepartmentHistoryAction action, String reason, UUID actor) {
        JsonNode previous = before == null ? null : json.valueToTree(snapshot(before));
        JsonNode next = json.valueToTree(snapshot(after));
        List<String> changedFields = changedFields(before, after);
        histories.save(new DepartmentHistory(null, after.id(), action, previous, next, reason, actor, null));
        audit.recordTenantEvent("DEPARTMENT_" + action.name(), "DEPARTMENT", after.id().toString(),
                Map.of("action", action.name(), "changedFields", changedFields));
    }

    private List<String> changedFields(Department before, Department after) {
        Map<String, Object> oldValues = before == null ? Map.of() : snapshot(before);
        Map<String, Object> newValues = snapshot(after);
        List<String> changed = new ArrayList<>();
        for (String field : newValues.keySet()) {
            if (!Objects.equals(oldValues.get(field), newValues.get(field))) changed.add(field);
        }
        return changed;
    }

    private Map<String, Object> snapshot(Department d) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("code", d.code());
        state.put("name", d.name());
        state.put("description", d.description());
        state.put("parentDepartmentId", d.parentDepartmentId());
        state.put("managerEmployeeId", d.managerEmployeeId());
        state.put("costCenterCode", d.costCenterCode());
        state.put("status", d.status().name());
        return state;
    }
}
