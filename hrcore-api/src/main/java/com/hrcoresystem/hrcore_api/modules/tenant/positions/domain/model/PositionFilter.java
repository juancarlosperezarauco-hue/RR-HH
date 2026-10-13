package com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.model;

public record PositionFilter(PositionStatus status, String name, String code,
                             OrganizationalLevel organizationalLevel) { }
