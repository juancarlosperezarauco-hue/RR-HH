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
import com.hrcoresystem.hrcore_api.common.exception.BusinessException;

@Component
public class LegacyPageableArgumentResolver implements HandlerMethodArgumentResolver {

  private final PageableHandlerMethodArgumentResolver delegate = new PageableHandlerMethodArgumentResolver(
      new SortHandlerMethodArgumentResolver());

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return Pageable.class.isAssignableFrom(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      @Nullable ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      @Nullable WebDataBinderFactory binderFactory) throws Exception {
    Pageable pageable = delegate.resolveArgument(parameter, mavContainer, webRequest, binderFactory);

    HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
    if (request == null) {
      return pageable;
    }

    Map<String, String[]> params = request.getParameterMap();
    boolean hasLimit = params.containsKey("limit"); boolean hasOffset = params.containsKey("offset");
    if ((hasLimit || hasOffset) && PaginationCompatibilitySupport.hasExplicitSpringPagingParameters(request)) throw new BusinessException("No combine page/size con limit/offset");
    if (PaginationCompatibilitySupport.hasExplicitSpringPagingParameters(request)) {
      return pageable;
    }

    Integer limit = readPositiveInt(params.get("limit"), "limit", 100);
    Integer offset = readNonNegativeInt(params.get("offset"), "offset", Integer.MAX_VALUE);

    if (limit == null && offset == null) {
      return pageable;
    }

    int size = limit != null ? limit : pageable.getPageSize();
    if (size <= 0) {
      size = pageable.getPageSize() > 0 ? pageable.getPageSize() : 50;
    }

    int safeOffset = offset != null ? offset : 0;
    return new OffsetPageable(safeOffset, size, pageable.getSort());
  }

  private Integer readPositiveInt(@Nullable String[] values, String name, int max) {
    if (values == null) return null; Integer value = readInt(values, name); if(value<1||value>max) throw new BusinessException(name+" debe estar entre 1 y "+max); return value;
  }

  private Integer readNonNegativeInt(@Nullable String[] values, String name, int max) {
    if (values == null) return null; Integer value = readInt(values, name); if(value<0||value>max) throw new BusinessException(name+" debe ser un número no negativo"); return value;
  }

  private Integer readInt(@Nullable String[] values, String name) {
    if (values == null || values.length == 0 || values[0] == null || values[0].isBlank()) throw new BusinessException(name+" debe ser numérico");

    try {
      return Integer.parseInt(values[0]);
    } catch (NumberFormatException ex) {
      throw new BusinessException(name+" debe ser numérico");
    }
  }
}
