package com.hrcoresystem.hrcore_api.common.security.authorization;

import com.hrcoresystem.hrcore_api.common.security.principal.AuthenticatedUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component("authorizationGuards")
public class AuthorizationGuards {

    private static final String PLATFORM_TENANT = "platform";

    public boolean isPlatformAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticated(authentication)) {
            return false;
        }

        AuthenticatedUserPrincipal principal = getPrincipal(authentication);
        if (principal == null) {
            return false;
        }

        return PLATFORM_TENANT.equalsIgnoreCase(principal.tenantSlug())
                && hasAnyAuthority(authentication, "ROLE_ADMIN", "ROLE_SUPERADMIN");
    }

    public boolean isPlatformAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticated(authentication)) {
            return false;
        }

        AuthenticatedUserPrincipal principal = getPrincipal(authentication);
        return principal != null && PLATFORM_TENANT.equalsIgnoreCase(principal.tenantSlug());
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated();
    }

    private AuthenticatedUserPrincipal getPrincipal(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUserPrincipal authenticatedUserPrincipal) {
            return authenticatedUserPrincipal;
        }
        return null;
    }

    private boolean hasAnyAuthority(Authentication authentication, String... authorities) {
        Set<String> currentAuthorities = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        for (String authority : authorities) {
            if (currentAuthorities.contains(authority)) {
                return true;
            }
        }

        return false;
    }
}
