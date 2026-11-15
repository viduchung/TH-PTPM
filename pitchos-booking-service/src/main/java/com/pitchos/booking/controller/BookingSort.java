package com.pitchos.booking.controller;

import com.pitchos.booking.exception.BadRequestException;
import org.springframework.data.domain.Sort;

import java.util.Locale;
import java.util.Set;

/** TH3 - Tham số sort dạng "truong,asc|desc", chỉ cho phép sắp xếp theo các cột trong danh sách trắng. */
final class BookingSort {

    static final String DEFAULT = "createdAt,desc";
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "createdAt", "startTime", "endTime", "totalPrice", "status", "pitchId");

    private BookingSort() {
    }

    static Sort parse(String sortParam) {
        String[] parts = sortParam.split(",");
        String field = parts[0].trim();
        if (!SORTABLE_FIELDS.contains(field) || parts.length > 2) {
            throw new BadRequestException("sort không hợp lệ. Cho phép: " + SORTABLE_FIELDS + " kèm ,asc hoặc ,desc");
        }
        String direction = parts.length == 2 ? parts[1].trim().toLowerCase(Locale.ROOT) : "asc";
        return switch (direction) {
            case "asc" -> Sort.by(field).ascending().and(Sort.by("id").ascending());
            case "desc" -> Sort.by(field).descending().and(Sort.by("id").descending());
            default -> throw new BadRequestException("Chiều sắp xếp chỉ được là asc hoặc desc");
        };
    }
}
