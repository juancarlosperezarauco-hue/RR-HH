package com.hrcoresystem.hrcore_api.modules.identity.access.application.usecase;

import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.SystemPermissionResponse;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.mapper.SystemPermissionMapper;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.SystemPermissionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListSystemPermissionsUseCase {

    private final SystemPermissionRepository systemPermissionRepository;
    private final SystemPermissionMapper systemPermissionMapper;

    public ListSystemPermissionsUseCase(
            SystemPermissionRepository systemPermissionRepository,
            SystemPermissionMapper systemPermissionMapper
    ) {
        this.systemPermissionRepository = systemPermissionRepository;
        this.systemPermissionMapper = systemPermissionMapper;
    }

    public List<SystemPermissionResponse> execute() {
        return systemPermissionRepository.findAllActive()
                .stream()
                .map(systemPermissionMapper::toResponse)
                .toList();
    }
}