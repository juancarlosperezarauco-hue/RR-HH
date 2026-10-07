package com.hrcoresystem.hrcore_api.common.pagination;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Map;

@Component
public class LegacyPageableArgumentResolver implements HandlerMethodArgumentResolver {

    private final PageableHandlerMethodArgumentResolver delegate =
            new PageableHandlerMethodArgumentResolver(new SortHandlerMethodArgumentResolver());

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return Pageable.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            @Nullable ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            @Nullable WebDataBinderFactory binderFactory
    ) throws Exception {
        Pageable pageable = delegate.resolveArgument(parameter, mavContainer, webRequest, binderFactory);

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            return pageable;
        }

        Map<String, String[]> params = request.getParameterMap();
        if (PaginationCompatibilitySupport.hasExplicitSpringPagingParameters(request)) {
            return pageable;
        }

        Integer limit = readPositiveInt(params.get("limit"));
        Integer offset = readNonNegativeInt(params.get("offset"));

        if (limit == null && offset == null) {
            return pageable;
        }

        int size = limit != null ? limit : pageable.getPageSize();
        if (size <= 0) {
            size = pageable.getPageSize() > 0 ? pageable.getPageSize() : 50;
        }

        int safeOffset = offset != null ? offset : 0;
        int page = size == 0 ? 0 : Math.max(safeOffset / size, 0);

        return PageRequest.of(page, size, pageable.getSort());
    }

    private Integer readPositiveInt(@Nullable String[] values) {
        Integer value = readInt(values);
        return value != null && value > 0 ? value : null;
    }

    private Integer readNonNegativeInt(@Nullable String[] values) {
        Integer value = readInt(values);
        return value != null && value >= 0 ? value : null;
    }

    private Integer readInt(@Nullable String[] values) {
        if (values == null || values.length == 0 || values[0] == null || values[0].isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(values[0]);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
