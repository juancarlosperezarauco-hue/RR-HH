package com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository;

import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.SystemPermission;

import java.util.List;
import java.util.Set;

public interface SystemPermissionRepository {

    List<SystemPermission> findAllActive();

    Set<String> findActiveCodes();
}