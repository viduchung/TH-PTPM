package com.football.data.dto.booking;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * DTO Cập nhật trạng thái đơn đặt sân (Duyệt / Từ chối / Hủy đơn)
 * Dùng cho các endpoint PATCH hoặc gọi liên dịch vụ khi thanh toán thành công.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingStatusUpdateDTO {

    @NotBlank(message = "Trạng thái mới không được để trống")
    private String status; // DA_DUYET, TU_CHOI, DA_HUY

    private String reason; // Lý do từ chối hoặc hủy đơn
}
