package com.football.data.mapper;

import com.football.data.booking.entity.Booking;
import com.football.data.booking.entity.BookingStatus;
import com.football.data.dto.booking.BookingRequestDTO;
import com.football.data.dto.booking.BookingResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class BookingMapperTest {

    @Test
    @DisplayName("BookingMapper: Chuyển đổi Entity sang BookingResponseDTO và tự tính durationHours")
    void testBookingToDTO() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 20, 18, 0);
        LocalDateTime end   = LocalDateTime.of(2026, 11, 20, 19, 30); // 1.5 giờ

        Booking booking = Booking.builder()
                .id(99L)
                .userId(1L)
                .pitchId(2L)
                .startTime(start)
                .endTime(end)
                .pricePerHour(new BigDecimal("300000.00"))
                .totalPrice(new BigDecimal("450000.00"))
                .status(BookingStatus.DA_DUYET)
                .note("Giao hữu")
                .build();

        BookingResponseDTO dto = BookingMapper.toDTO(booking, "Sân 7 A");

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(99L);
        assertThat(dto.getUserId()).isEqualTo(1L);
        assertThat(dto.getPitchId()).isEqualTo(2L);
        assertThat(dto.getPitchName()).isEqualTo("Sân 7 A");
        assertThat(dto.getDurationHours()).isEqualTo(1.5);
        assertThat(dto.getStatus()).isEqualTo("DA_DUYET");
        assertThat(dto.getStatusDescription()).isEqualTo("Đã duyệt");
        assertThat(dto.getTotalPrice()).isEqualByComparingTo("450000.00");
    }

    @Test
    @DisplayName("BookingMapper: Chuyển đổi BookingRequestDTO sang Entity Booking")
    void testRequestDTOToEntity() {
        BookingRequestDTO request = BookingRequestDTO.builder()
                .userId(5L)
                .pitchId(10L)
                .startTime(LocalDateTime.of(2026, 11, 25, 20, 0))
                .endTime(LocalDateTime.of(2026, 11, 25, 21, 30))
                .note("Đá giải")
                .build();

        Booking entity = BookingMapper.toEntity(request, new BigDecimal("350000.00"), new BigDecimal("525000.00"));

        assertThat(entity).isNotNull();
        assertThat(entity.getUserId()).isEqualTo(5L);
        assertThat(entity.getPitchId()).isEqualTo(10L);
        assertThat(entity.getPricePerHour()).isEqualByComparingTo("350000.00");
        assertThat(entity.getTotalPrice()).isEqualByComparingTo("525000.00");
        assertThat(entity.getStatus()).isEqualTo(BookingStatus.CHO_DUYET);
    }
}
