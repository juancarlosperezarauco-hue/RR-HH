package com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

final class AttendancePage {
    private AttendancePage() { }

    static Pageable of(int page, int size, String sort, String direction, String defaultSort, Set<String> allowed) {
        if (page < 0 || size < 1 || size > 100) throw new BusinessException("page must be nonnegative and size must be 1 to 100");
        String field = sort == null || sort.isBlank() ? defaultSort : sort.trim();
        if (!allowed.contains(field)) throw new BusinessException("Unsupported sort field: " + field);
        Sort.Direction order;
        try { order = Sort.Direction.fromString(direction == null ? "DESC" : direction); }
        catch (IllegalArgumentException ex) { throw new BusinessException("direction must be ASC or DESC"); }
        return PageRequest.of(page, size, Sort.by(order, field).and(Sort.by("id")));
    }
}
