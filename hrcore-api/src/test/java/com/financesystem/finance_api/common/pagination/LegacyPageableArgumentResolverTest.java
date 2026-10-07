package com.hrcoresystem.hrcore_api.common.pagination;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyPageableArgumentResolverTest {

    @Test
    void translatesLegacyLimitAndOffsetToPageable() throws Exception {
        LegacyPageableArgumentResolver resolver = new LegacyPageableArgumentResolver();
        MethodParameter parameter = pageableParameter();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("limit", "20");
        request.setParameter("offset", "40");

        Pageable pageable = (Pageable) resolver.resolveArgument(
                parameter,
                null,
                new ServletWebRequest(request),
                null
        );

        assertEquals(2, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
    }

    @Test
    void keepsSpringPagingParametersUntouched() throws Exception {
        LegacyPageableArgumentResolver resolver = new LegacyPageableArgumentResolver();
        MethodParameter parameter = pageableParameter();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("page", "3");
        request.setParameter("size", "15");

        Pageable pageable = (Pageable) resolver.resolveArgument(
                parameter,
                null,
                new ServletWebRequest(request),
                null
        );

        assertEquals(3, pageable.getPageNumber());
        assertEquals(15, pageable.getPageSize());
    }

    private MethodParameter pageableParameter() throws NoSuchMethodException {
        Method method = DummyController.class.getDeclaredMethod("handle", Pageable.class);
        return new MethodParameter(method, 0);
    }

    static class DummyController {
        @SuppressWarnings("unused")
        public void handle(Pageable pageable) {
        }
    }
}
