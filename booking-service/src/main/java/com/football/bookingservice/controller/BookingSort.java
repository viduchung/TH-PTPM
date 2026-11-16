package com.football.bookingservice.controller;

import com.football.bookingservice.exception.BadRequestException;
import org.springframework.data.domain.Sort;

import java.util.Locale;
import java.util.Set;

/** TH3 - Sắp xếp an toàn: chỉ cho sắp theo danh sách trường cố định, dạng "startTime,desc". */
final class BookingSort {

    static final String DEFAULT = "startTime,asc";
    private static final Set<String> ALLOWED_FIELDS =
            Set.of("id", "startTime", "endTime", "createdAt", "totalPrice", "status");

    private BookingSort() {
    }

    static Sort parse(String raw) {
        String[] parts = raw.split(",", -1);
        String field = parts[0].trim();
        if (!ALLOWED_FIELDS.contains(field) || parts.length > 2) {
            throw new BadRequestException("sort chỉ nhận một trong " + ALLOWED_FIELDS + ", dạng field,asc|desc");
        }
        Sort.Direction direction = parts.length == 2 ? parseDirection(parts[1]) : Sort.Direction.ASC;
        // Thêm id để thứ tự luôn xác định khi nhiều đơn cùng giá trị (phân trang không lặp / sót đơn)
        return Sort.by(direction, field).and(Sort.by(Sort.Direction.ASC, "id"));
    }

    private static Sort.Direction parseDirection(String raw) {
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new BadRequestException("Chiều sắp xếp chỉ nhận asc hoặc desc");
        };
    }
}
