package com.hrcoresystem.hrcore_api.modules.identity.access.application.validation;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRoleSystemNames;
import java.util.Locale;
import java.util.regex.Pattern;
public final class TenantRoleCodeValidator {
    private static final Pattern FORMAT=Pattern.compile("^[A-Z][A-Z0-9_]{0,99}$");
    private TenantRoleCodeValidator(){}
    public static String normalize(String value){
        if(value==null) throw new IllegalArgumentException("Role code is required");
        String code=value.trim().toUpperCase(Locale.ROOT);
        if(!FORMAT.matcher(code).matches() || code.startsWith("ROLE_") || TenantRoleSystemNames.isReservedCode(code) || "SUPERADMIN".equals(code)) throw new IllegalArgumentException("Invalid or reserved role code");
        return code;
    }
}
