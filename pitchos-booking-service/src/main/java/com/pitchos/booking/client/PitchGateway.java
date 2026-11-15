package com.pitchos.booking.client;

import com.pitchos.booking.dto.PitchResponseDTO;

/**
 * Cổng giao tiếp Booking (Consumer) → Pitch Service (Provider). Có 2 cách cài đặt, chọn bằng pitch.client.type:
 *   webclient (mặc định, TH2/TH6/TH7) · feign (OpenFeign cũ, giữ để tương thích).
 *
 * Hợp đồng chung cho mọi cách cài đặt:
 *   - Trả về sân đã được kiểm tra hợp lệ (id khớp, có giá > 0, có trạng thái).
 *   - Sân không tồn tại (404)                    → ResourceNotFoundException      (404 cho client)
 *   - Tắt / timeout / 5xx / dữ liệu sai / mã lạ  → PitchServiceUnavailableException (503 cho client)
 */
public interface PitchGateway {

    PitchResponseDTO getPitch(Long pitchId);
}
