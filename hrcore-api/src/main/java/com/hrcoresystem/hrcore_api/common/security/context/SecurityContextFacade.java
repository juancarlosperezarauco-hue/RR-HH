package com.hrcoresystem.hrcore_api.common.security.context;

import com.hrcoresystem.hrcore_api.common.security.principal.AuthenticatedUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SecurityContextFacade {

    public AuthenticatedUserPrincipal getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUserPrincipal authenticatedUserPrincipal) {
            return authenticatedUserPrincipal;
        }

        return null;
    }

    public String getCurrentSubject() {
        AuthenticatedUserPrincipal principal = getCurrentPrincipal();
        return principal != null ? principal.subject() : null;
    }

    public String getCurrentDisplayName() {
        AuthenticatedUserPrincipal principal = getCurrentPrincipal();
        if (principal == null) {
            return null;
        }

        String displayName = principal.displayName();
        return (displayName == null || displayName.isBlank()) ? null : displayName;
    }

    public String getCurrentEmail() {
        AuthenticatedUserPrincipal principal = getCurrentPrincipal();
        if (principal == null) {
            return null;
        }

        String email = principal.email();
        return (email == null || email.isBlank()) ? null : email;
    }

    public String getCurrentTenantSlug() {
        AuthenticatedUserPrincipal principal = getCurrentPrincipal();
        return principal != null ? principal.tenantSlug() : null;
    }

    public boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authority == null || authority.isBlank()) {
            return false;
        }

        Set<String> currentAuthorities = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        return currentAuthorities.contains(authority);
    }
}
