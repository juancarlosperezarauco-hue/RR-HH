package com.hrcoresystem.hrcore_api.modules.identity.users.application.usecase;

import com.hrcoresystem.hrcore_api.modules.identity.users.application.dto.TenantUserResponse;
import com.hrcoresystem.hrcore_api.modules.identity.users.application.mapper.TenantUserMapper;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUserStatus;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListTenantUsersUseCase {

    private final TenantUserRepository tenantUserRepository;
    private final TenantUserMapper tenantUserMapper;

    public ListTenantUsersUseCase(
            TenantUserRepository tenantUserRepository,
            TenantUserMapper tenantUserMapper
    ) {
        this.tenantUserRepository = tenantUserRepository;
        this.tenantUserMapper = tenantUserMapper;
    }

    public List<TenantUserResponse> execute() {
        return execute(null, null, null);
    }

    public List<TenantUserResponse> execute(String search, TenantUserStatus status, Boolean active) {
        String query = search != null ? search.trim().toLowerCase() : null;

        return tenantUserRepository.findAll()
                .stream()
                .filter(user -> {
                    if (query != null && !query.isEmpty()) {
                        String fullName = ((user.firstName() != null ? user.firstName() : "") + " "
                                + (user.lastName() != null ? user.lastName() : "")).toLowerCase();
                        boolean matchesEmail = user.email() != null && user.email().toLowerCase().contains(query);
                        boolean matchesName = fullName.contains(query);
                        boolean matchesId = user.id() != null && user.id().toString().contains(query);
                        if (!matchesEmail && !matchesName && !matchesId) {
                            return false;
                        }
                    }

                    if (status != null && user.status() != status) {
                        return false;
                    }

                    if (active != null && user.active() != active) {
                        return false;
                    }

                    return true;
                })
                .map(tenantUserMapper::toResponse)
                .toList();
    }
}
