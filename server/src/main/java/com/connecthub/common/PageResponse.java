package com.connecthub.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Offset-paginated list. Used where random access matters more than feed-style scrolling. */
public record PageResponse<T>(List<T> items, int page, int size, long totalElements, boolean hasNext) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.hasNext());
    }
}
