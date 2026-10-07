package com.hrcoresystem.hrcore_api.common.pagination;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;

import java.util.Map;

public final class PaginationCompatibilitySupport {

    private PaginationCompatibilitySupport() {
    }

    public static boolean hasExplicitSpringPagingParameters(HttpServletRequest request) {
        if (request == null) {
            return false;
        }

        Map<String, String[]> params = request.getParameterMap();
        return params.containsKey("page")
                || params.containsKey("size")
                || params.containsKey("sort");
    }

    public static Object adaptResponseData(Object data, HttpServletRequest request) {
        if (data instanceof Page<?> page && !hasExplicitSpringPagingParameters(request)) {
            return page.getContent();
        }

        return data;
    }
}
