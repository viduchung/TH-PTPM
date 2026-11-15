package com.pitchos.booking.client;

import com.pitchos.booking.dto.PitchResponseDTO;
import com.pitchos.booking.exception.PitchServiceUnavailableException;

import java.math.BigDecimal;

/** Không tin dữ liệu từ service khác: thiếu id, id lệch, thiếu giá, giá ≤ 0 hoặc thiếu trạng thái → 503. */
final class PitchResponseValidator {

    static final String INVALID_DATA_MESSAGE =
            "Dữ liệu sân trả về từ dịch vụ quản lý sân không hợp lệ, chưa thể đặt sân. Vui lòng thử lại sau!";

    private PitchResponseValidator() {
    }

    static PitchResponseDTO requireValid(Long requestedId, PitchResponseDTO pitch) {
        boolean isValid = pitch != null
                && requestedId.equals(pitch.getId())
                && pitch.getPricePerHour() != null
                && pitch.getPricePerHour().compareTo(BigDecimal.ZERO) > 0
                && pitch.getStatus() != null;
        if (!isValid) {
            throw new PitchServiceUnavailableException(INVALID_DATA_MESSAGE);
        }
        return pitch;
    }
}
