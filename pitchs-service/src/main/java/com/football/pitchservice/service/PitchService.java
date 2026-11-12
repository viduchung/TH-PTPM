package com.football.pitchservice.service;

import com.football.pitchservice.dto.PitchRequestDTO;
import com.football.pitchservice.dto.PitchResponseDTO;
import com.football.pitchservice.dto.PriceResponseDTO;
import com.football.pitchservice.entity.Pitch;

import java.util.List;

public interface PitchService {

    // Lấy toàn bộ danh sách sân dưới dạng DTO
    List<PitchResponseDTO> getAllPitches();

    // Lấy chi tiết sân theo ID (cho booking-service xem chi tiết)
    PitchResponseDTO getPitchById(Long id);

    // Endpoint chuyên biệt cho booking-service: Lấy thông tin giá và tính khả dụng của sân
    PriceResponseDTO getPitchPrice(Long id);

    // Thêm mới sân bóng
    PitchResponseDTO createPitch(PitchRequestDTO request);

    // Cập nhật thông tin sân bóng
    PitchResponseDTO updatePitch(Long id, PitchRequestDTO request);

    // Xóa sân bóng
    void deletePitch(Long id);

    // Phương thức nội bộ tìm entity
    Pitch findEntityById(Long id);
}
