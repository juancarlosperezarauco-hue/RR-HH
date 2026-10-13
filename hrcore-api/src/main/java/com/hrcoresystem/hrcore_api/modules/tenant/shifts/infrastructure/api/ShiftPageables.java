package com.hrcoresystem.hrcore_api.modules.tenant.shifts.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

final class ShiftPageables {
    private ShiftPageables() {}

    static Pageable checked(Pageable pageable, Set<String> allowed, String defaultSort) {
        if (pageable.isUnpaged() || pageable.getPageNumber() < 0
                || pageable.getPageSize() < 1 || pageable.getPageSize() > 100) {
            throw new BusinessException("Page size must be between 1 and 100");
        }
        for (Sort.Order order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw new BusinessException("Unsupported sort field: " + order.getProperty());
            }
        }
        Sort sort = pageable.getSort().isUnsorted()
                ? Sort.by(defaultSort).ascending() : pageable.getSort();
        if (sort.getOrderFor("id") == null) {
            sort = sort.and(Sort.by("id").ascending());
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}
