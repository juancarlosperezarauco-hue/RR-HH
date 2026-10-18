package com.hrcoresystem.hrcore_api.common.pagination;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;

public final class PaginationSupport {

  private static final int DEFAULT_PAGE_SIZE = 50;

  private PaginationSupport() {
  }

  public static <T> Page<T> page(List<T> items, Pageable pageable) {
    List<T> safeItems = items == null ? Collections.emptyList() : items;
    Pageable safePageable = normalize(pageable);

    int page = safePageable.getPageNumber();
    int size = safePageable.getPageSize();
    int fromIndex = (int)Math.min(safePageable.getOffset(), safeItems.size());
    int toIndex = Math.min(fromIndex + size, safeItems.size());

    return new PageImpl<>(
        safeItems.subList(fromIndex, toIndex),
        safePageable,
        safeItems.size());
  }

  private static Pageable normalize(Pageable pageable) {
    if (pageable == null) {
      return PageRequest.of(0, DEFAULT_PAGE_SIZE);
    }

    if (pageable instanceof OffsetPageable) return pageable;
    int page = Math.max(pageable.getPageNumber(), 0);
    int size = pageable.getPageSize() > 0 ? pageable.getPageSize() : DEFAULT_PAGE_SIZE;
    return PageRequest.of(page, size, pageable.getSort());
  }
}
