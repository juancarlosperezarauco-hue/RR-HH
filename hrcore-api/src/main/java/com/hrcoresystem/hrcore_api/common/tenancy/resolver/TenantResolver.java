package com.hrcoresystem.hrcore_api.common.tenancy.resolver;

import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import jakarta.servlet.http.HttpServletRequest;

public interface TenantResolver {

    TenantContext resolve(HttpServletRequest request);
}