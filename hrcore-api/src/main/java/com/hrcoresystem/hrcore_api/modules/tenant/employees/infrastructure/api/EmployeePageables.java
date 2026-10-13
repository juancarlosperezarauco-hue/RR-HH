package com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import org.springframework.data.domain.*;
import java.util.Set;

final class EmployeePageables {
    private EmployeePageables() { }
    static Pageable checked(Pageable pageable, Set<String> allowed, String defaultField) {
        if (pageable.isUnpaged() || pageable.getPageNumber() < 0 || pageable.getPageSize() < 1 || pageable.getPageSize() > 100)
            throw new BusinessException("Page size must be between 1 and 100");
        for (Sort.Order order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) throw new BusinessException("Unsupported sort field: " + order.getProperty());
        }
        Sort sort = pageable.getSort().isUnsorted() ? Sort.by(defaultField).ascending() : pageable.getSort();
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort.and(Sort.by("id").ascending()));
    }
}
