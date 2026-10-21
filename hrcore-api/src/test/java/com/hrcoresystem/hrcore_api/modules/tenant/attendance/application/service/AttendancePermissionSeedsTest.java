package com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hrcoresystem.hrcore_api.modules.identity.access.application.catalog.PermissionCatalogRegistry;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.PermissionDefinition;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.PermissionScope;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class AttendancePermissionSeedsTest {
    @Test
    void permissionCatalogRegistersExactlyTheAttendanceAuthorities() {
        Set<String> expected = Set.of(
                "attendance.records.list", "attendance.records.detail", "attendance.records.calculate",
                "attendance.team.read", "attendance.punches.create", "attendance.punches.manage",
                "attendance.punches.list", "attendance.punches.detail", "attendance.adjustments.create",
                "attendance.adjustments.list", "attendance.adjustments.detail", "attendance.adjustments.approve",
                "attendance.adjustments.reject", "attendance.adjustments.cancel", "attendance.overtime.create",
                "attendance.overtime.list", "attendance.overtime.detail", "attendance.overtime.approve",
                "attendance.overtime.reject", "attendance.overtime.cancel");

        Set<PermissionDefinition> definitions = Set.copyOf(PermissionCatalogRegistry.definitions());
        Set<String> attendanceCodes = definitions.stream()
                .map(PermissionDefinition::code)
                .filter(code -> code.startsWith("attendance."))
                .collect(Collectors.toSet());

        assertEquals(expected, attendanceCodes);
        assertTrue(definitions.stream()
                .filter(permission -> expected.contains(permission.code()))
                .allMatch(permission -> permission.scope() == PermissionScope.TENANT && permission.active()));
    }
}
