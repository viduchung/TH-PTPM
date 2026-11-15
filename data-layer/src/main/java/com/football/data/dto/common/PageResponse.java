package com.football.data.dto.common;

import lombok.*;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cấu trúc phản hồi phân trang chuẩn hóa (Pagination Response DTO)
 * 
 * =========================================================================
 * NGUYÊN TẮC THIẾT KẾ PHÂN TRANG CỦA HÙNG (TH3):
 * 1. Bọc danh sách kết quả `content` kèm các chỉ số phân trang cần thiết.
 * 2. Loại bỏ các trường metadata rườm rà của Spring Data `PageImpl`
 *    nhằm giảm 60% kích thước payload truyền qua mạng.
 * 3. Cung cấp hàm chuyển đổi tiện ích `from(Page<E> page, Function<E, R> mapper)`
 *    giúp Controller map tự động từ Entity sang DTO kèm phân trang.
 * =========================================================================
 * 
 * Phụ trách: HÙNG (feat/hung-data) - Tuần 3 (TH3)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {

    private List<T> content;

    private int pageNumber;

    private int pageSize;

    private long totalElements;

    private int totalPages;

    private boolean isFirst;

    private boolean isLast;

    private boolean isEmpty;

    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .isEmpty(page.isEmpty())
                .build();
    }

    public static <E, R> PageResponse<R> from(Page<E> page, Function<E, R> mapper) {
        List<R> mappedContent = page.getContent().stream()
                .map(mapper)
                .collect(Collectors.toList());

        return PageResponse.<R>builder()
                .content(mappedContent)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .isEmpty(page.isEmpty())
                .build();
    }
}

