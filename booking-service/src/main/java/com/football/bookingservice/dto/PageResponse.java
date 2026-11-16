package com.football.bookingservice.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Phân trang gọn (TH3) - cùng tên trường JSON với PageResponse của data-layer. */
public record PageResponse<T>(List<T> content, int pageNumber, int pageSize, long totalElements,
                              int totalPages, boolean first, boolean last, boolean empty) {

    public static <E, R> PageResponse<R> from(Page<E> page, Function<E, R> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(),
                page.isFirst(), page.isLast(), page.isEmpty());
    }
}
