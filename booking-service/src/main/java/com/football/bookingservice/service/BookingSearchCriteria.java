package com.football.bookingservice.service;

import com.football.bookingservice.entity.Booking;
import com.football.bookingservice.entity.BookingStatus;
import com.football.bookingservice.exception.BadRequestException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * TH3 - Bộ lọc danh sách đơn. Mọi điều kiện đều tùy chọn, ghép bằng AND, lọc theo GIỜ BẮT ĐẦU của đơn.
 * date = cả một ngày; from / to = khoảng [from, to).
 */
public record BookingSearchCriteria(Long userId, Long pitchId, BookingStatus status,
                                    LocalDate date, LocalDateTime from, LocalDateTime to) {

    public BookingSearchCriteria {
        if (from != null && to != null && !to.isAfter(from)) {
            throw new BadRequestException("Tham số 'to' phải sau 'from'");
        }
    }

    public BookingSearchCriteria withUserId(long ownerUserId) {
        return new BookingSearchCriteria(ownerUserId, pitchId, status, date, from, to);
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
            if (date != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startTime"), date.atStartOfDay()));
                predicates.add(cb.lessThan(root.get("startTime"), date.plusDays(1).atStartOfDay()));
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
