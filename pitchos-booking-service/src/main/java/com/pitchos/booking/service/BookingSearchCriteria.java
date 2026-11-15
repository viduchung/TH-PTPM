package com.pitchos.booking.service;

import com.pitchos.booking.entity.Booking;
import com.pitchos.booking.entity.BookingStatus;
import com.pitchos.booking.exception.BadRequestException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * TH3 - Điều kiện lọc danh sách đơn. Lọc theo ngày/khoảng thời gian dựa trên giờ BẮT ĐẦU đá: from ≤ startTime < to.
 * date=yyyy-MM-dd là cách viết tắt của from = 00:00 ngày đó, to = 00:00 ngày hôm sau.
 */
public record BookingSearchCriteria(Long userId, Long pitchId, BookingStatus status,
                                    LocalDateTime from, LocalDateTime to) {

    public static BookingSearchCriteria of(Long userId, Long pitchId, BookingStatus status,
                                           LocalDate date, LocalDateTime from, LocalDateTime to) {
        if (date != null && (from != null || to != null)) {
            throw new BadRequestException("Chỉ dùng date hoặc cặp from/to, không dùng cả hai");
        }
        LocalDateTime effectiveFrom = date != null ? date.atStartOfDay() : from;
        LocalDateTime effectiveTo = date != null ? date.plusDays(1).atStartOfDay() : to;
        if (effectiveFrom != null && effectiveTo != null && !effectiveFrom.isBefore(effectiveTo)) {
            throw new BadRequestException("from phải trước to");
        }
        return new BookingSearchCriteria(userId, pitchId, status, effectiveFrom, effectiveTo);
    }

    public BookingSearchCriteria withUserId(Long forcedUserId) {
        return new BookingSearchCriteria(forcedUserId, pitchId, status, from, to);
    }

    public Specification<Booking> toSpecification() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (pitchId != null) {
                predicates.add(cb.equal(root.get("pitchId"), pitchId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startTime"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("startTime"), to));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
