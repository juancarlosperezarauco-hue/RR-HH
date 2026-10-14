package com.hrcoresystem.hrcore_api.common.security.session;
import com.hrcoresystem.hrcore_api.common.security.session.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
@Service
public class AuthSessionManagementService {
    private final TenantAuthSessionRepository tenant;
    private final PlatformAuthSessionRepository platform;
    public AuthSessionManagementService(TenantAuthSessionRepository tenant, PlatformAuthSessionRepository platform){this.tenant=tenant;this.platform=platform;}
    public List<AuthSessionResponse> tenantSessions(UUID userId, UUID current){return tenant.findByUserId(userId).stream().map(s->AuthSessionResponse.from(s,current)).toList();}
    public List<AuthSessionResponse> platformSessions(UUID userId, UUID current){return platform.findByUserId(userId).stream().map(s->AuthSessionResponse.from(s,current)).toList();}
    @Transactional public void revokeTenant(UUID sessionId, UUID userId){tenant.revokeOwnedSession(sessionId,userId);}
    @Transactional public void revokePlatform(UUID sessionId, UUID userId){platform.revokeOwnedSession(sessionId,userId);}
    @Transactional public void revokeOtherTenant(UUID userId, UUID current){tenant.revokeOtherSessions(userId,current);}
    @Transactional public void revokeOtherPlatform(UUID userId, UUID current){platform.revokeOtherSessions(userId,current);}
}
