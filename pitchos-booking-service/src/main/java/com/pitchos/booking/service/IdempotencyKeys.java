package com.pitchos.booking.service;

import com.pitchos.booking.entity.Booking;
import com.pitchos.booking.exception.BadRequestException;

import java.util.regex.Pattern;

/** TH6 - Kiểm tra header Idempotency-Key (tùy chọn: không gửi thì đặt sân như cũ). */
public final class IdempotencyKeys {

    public static final String HEADER = "Idempotency-Key";
    public static final String REPLAYED_HEADER = "Idempotent-Replayed";
    /** Cho phép UUID và các chuỗi an toàn khác, tối đa 64 ký tự (khớp độ dài cột idempotency_key). */
    private static final Pattern VALID_KEY =
            Pattern.compile("^[A-Za-z0-9._:-]{1," + Booking.IDEMPOTENCY_KEY_MAX_LENGTH + "}$");

    private IdempotencyKeys() {
    }

    /** null nếu không gửi; ném 400 nếu sai định dạng. */
    public static String normalize(String rawKey) {
        if (rawKey == null) {
            return null;
        }
        String key = rawKey.trim();
        if (!VALID_KEY.matcher(key).matches()) {
            throw new BadRequestException(HEADER + " chỉ gồm chữ, số, . _ : - và dài 1-"
                    + Booking.IDEMPOTENCY_KEY_MAX_LENGTH + " ký tự");
        }
        return key;
    }
}
