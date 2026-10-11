package com.hrcoresystem.hrcore_api.common.pagination;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaginationCompatibilitySupportTest {

    @Test
    void adaptsPageToListWhenNoSpringPagingParametersArePresent() {
        Page<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 2), 5);
        MockHttpServletRequest request = new MockHttpServletRequest();

        Object adapted = PaginationCompatibilitySupport.adaptResponseData(page, request);

        assertTrue(adapted instanceof List<?>);
        assertEquals(List.of("a", "b"), adapted);
    }

    @Test
    void keepsPageWhenSpringPagingParametersArePresent() {
        Page<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 2), 5);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("page", "0");
        request.setParameter("size", "2");

        Object adapted = PaginationCompatibilitySupport.adaptResponseData(page, request);

        assertSame(page, adapted);
    }
}
