package com.football.pitchservice.service;

import com.football.pitchservice.dto.*;
import com.football.pitchservice.entity.Pitch;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface PitchService {

    // TH3: Tìm kiếm, lọc danh sách sân kết hợp phân trang
    PageResponseDTO<PitchResponseDTO> searchPitches(
            String type,
            String status,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    // Lấy toàn bộ danh sách sân dưới dạng DTO
    List<PitchResponseDTO> getAllPitches();

    // Lấy chi tiết sân theo ID (cho booking-service xem chi tiết)
    PitchResponseDTO getPitchById(Long id);

    // Báo giá cơ bản
    PriceResponseDTO getPitchPrice(Long id);

    // TH6: API Provider /quote - Tính toán báo giá thuê sân chi tiết kèm giá giờ vàng
    QuoteResponseDTO calculateQuote(Long pitchId, LocalDate bookingDate, LocalTime startTime, LocalTime endTime);

    // Thêm mới sân bóng (Yêu cầu quyền OWNER / ADMIN)
    PitchResponseDTO createPitch(PitchRequestDTO request);

    // Cập nhật thông tin sân bóng (Yêu cầu quyền OWNER / ADMIN)
    PitchResponseDTO updatePitch(Long id, PitchRequestDTO request);

    // Xóa sân bóng (Yêu cầu quyền OWNER / ADMIN)
    void deletePitch(Long id);

    // TH4: Thêm khung giờ vàng cho một sân cụ thể
    GoldenHourPriceResponseDTO addGoldenHourPrice(Long pitchId, GoldenHourPriceRequestDTO request);

    // TH4: Xóa khung giờ vàng
    void deleteGoldenHourPrice(Long pitchId, Long priceId);

    // Phương thức nội bộ tìm entity
    Pitch findEntityById(Long id);
}
