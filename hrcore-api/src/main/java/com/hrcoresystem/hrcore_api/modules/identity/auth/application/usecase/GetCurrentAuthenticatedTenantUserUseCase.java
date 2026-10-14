package com.hrcoresystem.hrcore_api.modules.identity.auth.application.usecase;

import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantUserRoleRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRoleRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.service.TenantAuthorizationSnapshotService;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto.AuthenticatedRoleResponse;
import com.hrcoresystem.hrcore_api.common.security.authorization.domain.repository.TenantAuthorizationStateRepository;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto.AuthenticatedTenantUserResponse;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.exception.AuthenticationFailedException;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GetCurrentAuthenticatedTenantUserUseCase {

    private final SecurityContextFacade securityContextFacade;
    private final TenantUserRepository tenantUserRepository;
    private final TenantUserRoleRepository tenantUserRoleRepository;
    private final TenantRoleRepository tenantRoleRepository;
    private final TenantAuthorizationStateRepository authorizationStateRepository;
    private final TenantAuthorizationSnapshotService snapshotService;

    public GetCurrentAuthenticatedTenantUserUseCase(
            SecurityContextFacade securityContextFacade,
            TenantUserRepository tenantUserRepository,
            TenantUserRoleRepository tenantUserRoleRepository,
            TenantRoleRepository tenantRoleRepository,
            TenantAuthorizationStateRepository authorizationStateRepository,
            TenantAuthorizationSnapshotService snapshotService
    ) {
        this.securityContextFacade = securityContextFacade;
        this.tenantUserRepository = tenantUserRepository;
        this.tenantUserRoleRepository = tenantUserRoleRepository;
        this.tenantRoleRepository = tenantRoleRepository;
        this.authorizationStateRepository = authorizationStateRepository;
        this.snapshotService = snapshotService;
    }

    public AuthenticatedTenantUserResponse execute() {
        String currentSubject = securityContextFacade.getCurrentSubject();
        String currentTenantSlug = securityContextFacade.getCurrentTenantSlug();

        if (currentSubject == null || currentSubject.isBlank()) {
            throw new AuthenticationFailedException("Authenticated subject is not available");
        }

        TenantUser tenantUser = tenantUserRepository.findById(parseSubjectAsUserId(currentSubject))
                .orElseThrow(() -> new AuthenticationFailedException("Authenticated user not found"));

        if (!tenantUser.active()) throw new AuthenticationFailedException("Authenticated user is inactive");
        var snapshot = snapshotService.getSnapshot(tenantUser.id());
        var roleDetails = tenantUserRoleRepository.findRoleIdsByUserId(tenantUser.id()).stream()
                .map(tenantRoleRepository::findById).flatMap(java.util.Optional::stream)
                .filter(r -> r.active())
                .map(r -> new AuthenticatedRoleResponse(r.id(), r.code(), r.name(), r.systemRole()))
                .toList();
        var state = authorizationStateRepository.find().orElseThrow(() -> new AuthenticationFailedException("Tenant authorization state is missing"));
        boolean owner = state.ownerUserId() != null && state.ownerUserId().equals(tenantUser.id())
                && tenantUser.status().name().equals("ACTIVE")
                && roleDetails.stream().anyMatch(r -> r.systemRole() && r.code().equals("OWNER_ADMIN"));

        return new AuthenticatedTenantUserResponse(
                tenantUser.id(),
                tenantUser.email(),
                tenantUser.firstName(),
                tenantUser.lastName(),
                tenantUser.active(),
                tenantUser.status().name(),
                currentTenantSlug,
                "TENANT",
                owner,
                snapshot.roleCodes(),
                roleDetails,
                snapshot.permissionCodes(),
                snapshot.securityVersion(),
                snapshot.authorizationVersion(),
                snapshot.catalogVersion()
        );
    }

    private UUID parseSubjectAsUserId(String subject) {
        try {
            return UUID.fromString(subject.trim());
        } catch (IllegalArgumentException exception) {
            throw new AuthenticationFailedException("Authenticated subject is not a valid user id");
        }
    }
}
