package com.hrcoresystem.hrcore_api.common.security.principal;

import java.util.List;
import java.util.UUID;

public record AuthenticatedUserPrincipal(
        UUID subjectId,
        UUID sessionId,
        String email,
        String displayName,
        String tenantSlug,
        String scope,
        List<String> roles,
        List<String> permissions
        , long securityVersion, Long authorizationVersion, long catalogVersion
) {
    public AuthenticatedUserPrincipal(String subject,String email,String displayName,String tenantSlug,List<String> roles,List<String> permissions){this(UUID.fromString(subject),null,email,displayName,tenantSlug,"TENANT",roles,permissions,1,1L,1);}
    public String subject(){return subjectId.toString();}
}
