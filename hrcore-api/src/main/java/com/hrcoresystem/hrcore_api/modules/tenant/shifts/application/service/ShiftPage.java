package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

final class ShiftPage {
    private ShiftPage() { }

    static Pageable of(int page, int size, String sort, String direction,
                       String defaultSort, Set<String> allowed) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException("page must be nonnegative and size must be between 1 and 100");
        }
        String field = sort == null || sort.isBlank() ? defaultSort : sort.trim();
        if (!allowed.contains(field)) {
            throw new BusinessException("Unsupported sort field: " + field);
        }
        String order = direction == null || direction.isBlank() ? "ASC" : direction.trim();
        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(order);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("direction must be ASC or DESC");
        }
        return PageRequest.of(page, size, Sort.by(sortDirection, field).and(Sort.by("id")));
    }
}
