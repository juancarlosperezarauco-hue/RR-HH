package com.hrcoresystem.hrcore_api.modules.identity.access.application.service;

import com.hrcoresystem.hrcore_api.common.security.authorization.domain.PermissionCodeValidator;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.exception.InvalidSystemPermissionException;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.SystemPermissionRepository;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class PermissionAssignmentPolicy {
    private final SystemPermissionRepository repository;
    public PermissionAssignmentPolicy(SystemPermissionRepository repository) { this.repository = repository; }
    public void validate(List<String> codes) {
        if (codes == null) return;
        Set<String> seen = new HashSet<>();
        Set<String> allowed = repository.findActiveAssignableTenantCodes();
        for (String code : codes) {
            if (code == null || code.isBlank() || !PermissionCodeValidator.isValid(code) || !seen.add(code) || !allowed.contains(code))
                throw new InvalidSystemPermissionException("Permission is not active, tenant-scoped and assignable: " + code);
        }
    }
}
