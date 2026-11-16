package com.football.pitchservice.service.impl;

import com.football.pitchservice.dto.*;
import com.football.pitchservice.entity.GoldenHourPrice;
import com.football.pitchservice.entity.Pitch;
import com.football.pitchservice.exception.BadRequestException;
import com.football.pitchservice.exception.ResourceNotFoundException;
import com.football.pitchservice.repository.GoldenHourPriceRepository;
import com.football.pitchservice.repository.PitchRepository;
import com.football.pitchservice.service.PitchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PitchServiceImpl implements PitchService {

    private final PitchRepository pitchRepository;
    private final GoldenHourPriceRepository goldenHourPriceRepository;

    public PitchServiceImpl(PitchRepository pitchRepository, GoldenHourPriceRepository goldenHourPriceRepository) {
        this.pitchRepository = pitchRepository;
        this.goldenHourPriceRepository = goldenHourPriceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<PitchResponseDTO> searchPitches(
            String type,
            String status,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Pitch> pitchPage = pitchRepository.searchPitches(type, status, pageable);
        Page<PitchResponseDTO> dtoPage = pitchPage.map(PitchResponseDTO::fromEntity);

        return PageResponseDTO.fromPage(dtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PitchResponseDTO> getAllPitches() {
        return pitchRepository.findAll()
                .stream()
                .map(PitchResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PitchResponseDTO getPitchById(Long id) {
        Pitch pitch = findEntityById(id);
        return PitchResponseDTO.fromEntity(pitch);
    }

    @Override
    @Transactional(readOnly = true)
    public PriceResponseDTO getPitchPrice(Long id) {
        Pitch pitch = findEntityById(id);
        return PriceResponseDTO.fromEntity(pitch);
    }

    // ============================================================
    // TH6: TÍNH TOÁN BÁO GIÁ THUÊ SÂN KÈM LOGIC GIỜ VÀNG
    // ============================================================
    @Override
    @Transactional(readOnly = true)
    public QuoteResponseDTO calculateQuote(Long pitchId, LocalDate bookingDate, LocalTime startTime, LocalTime endTime) {
        if (bookingDate == null) {
            bookingDate = LocalDate.now();
        }

        if (startTime == null || endTime == null) {
            throw new BadRequestException("Giờ bắt đầu (startTime) và giờ kết thúc (endTime) không được để trống!");
        }

        if (!endTime.isAfter(startTime)) {
            throw new BadRequestException("Giờ kết thúc (" + endTime + ") phải diễn ra sau giờ bắt đầu (" + startTime + ")!");
        }

        long totalMinutes = ChronoUnit.MINUTES.between(startTime, endTime);
        if (totalMinutes < 30) {
            throw new BadRequestException("Thời lượng thuê sân tối thiểu là 30 phút!");
        }

        Pitch pitch = findEntityById(pitchId);
        if ("MAINTENANCE".equalsIgnoreCase(pitch.getStatus())) {
            throw new BadRequestException("Sân bóng '" + pitch.getName() + "' hiện đang bảo dưỡng, không thể đặt lịch hoặc báo giá!");
        }

        DayOfWeek dow = bookingDate.getDayOfWeek();
        boolean isWeekend = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);

        long goldenMinutes = 0;
        BigDecimal appliedGoldenPrice = null;

        if (pitch.getGoldenHourPrices() != null) {
            for (GoldenHourPrice ghp : pitch.getGoldenHourPrices()) {
                String ruleDay = ghp.getDayOfWeek();
                boolean matchesDay = "ALL_DAYS".equalsIgnoreCase(ruleDay)
                        || ("WEEKEND".equalsIgnoreCase(ruleDay) && isWeekend)
                        || ("WEEKDAY".equalsIgnoreCase(ruleDay) && !isWeekend)
                        || ruleDay.equalsIgnoreCase(dow.name());

                if (matchesDay) {
                    LocalTime overlapStart = startTime.isAfter(ghp.getStartTime()) ? startTime : ghp.getStartTime();
                    LocalTime overlapEnd = endTime.isBefore(ghp.getEndTime()) ? endTime : ghp.getEndTime();

                    if (overlapStart.isBefore(overlapEnd)) {
                        long overlap = ChronoUnit.MINUTES.between(overlapStart, overlapEnd);
                        goldenMinutes += overlap;
                        appliedGoldenPrice = ghp.getPricePerHour();
                    }
                }
            }
        }

        goldenMinutes = Math.min(goldenMinutes, totalMinutes);
        long regularMinutes = totalMinutes - goldenMinutes;

        BigDecimal basePrice = pitch.getPricePerHour();
        BigDecimal regularAmount = basePrice
                .multiply(BigDecimal.valueOf(regularMinutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        BigDecimal goldenAmount = BigDecimal.ZERO;
        if (goldenMinutes > 0 && appliedGoldenPrice != null) {
            goldenAmount = appliedGoldenPrice
                    .multiply(BigDecimal.valueOf(goldenMinutes))
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        }

        BigDecimal totalAmount = regularAmount.add(goldenAmount);

        String breakdown = String.format(
                "Tổng %d phút (Giờ thường: %d phút = %s đ | Giờ vàng: %d phút = %s đ)",
                totalMinutes,
                regularMinutes,
                regularAmount.toPlainString(),
                goldenMinutes,
                goldenAmount.toPlainString()
        );

        return new QuoteResponseDTO(
                pitch.getId(),
                pitch.getName(),
                pitch.getType(),
                bookingDate,
                startTime,
                endTime,
                totalMinutes,
                basePrice,
                regularMinutes,
                goldenMinutes,
                appliedGoldenPrice,
                totalAmount,
                goldenMinutes > 0,
                "VND",
                breakdown
        );
    }

    @Override
    public PitchResponseDTO createPitch(PitchRequestDTO request) {
        Pitch pitch = request.toEntity();
        Pitch savedPitch = pitchRepository.save(pitch);
        return PitchResponseDTO.fromEntity(savedPitch);
    }

    @Override
    public PitchResponseDTO updatePitch(Long id, PitchRequestDTO request) {
        Pitch existingPitch = findEntityById(id);

        existingPitch.setName(request.getName());
        existingPitch.setType(request.getType());
        existingPitch.setPricePerHour(request.getPricePerHour());
        existingPitch.setAddress(request.getAddress());
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            existingPitch.setStatus(request.getStatus());
        }
        existingPitch.setDescription(request.getDescription());

        Pitch updatedPitch = pitchRepository.save(existingPitch);
        return PitchResponseDTO.fromEntity(updatedPitch);
    }

    @Override
    public void deletePitch(Long id) {
        Pitch existingPitch = findEntityById(id);
        pitchRepository.delete(existingPitch);
    }

    @Override
    public GoldenHourPriceResponseDTO addGoldenHourPrice(Long pitchId, GoldenHourPriceRequestDTO request) {
        Pitch pitch = findEntityById(pitchId);
        GoldenHourPrice ghp = request.toEntity(pitch);
        pitch.addGoldenHourPrice(ghp);
        GoldenHourPrice saved = goldenHourPriceRepository.save(ghp);
        return GoldenHourPriceResponseDTO.fromEntity(saved);
    }

    @Override
    public void deleteGoldenHourPrice(Long pitchId, Long priceId) {
        Pitch pitch = findEntityById(pitchId);
        GoldenHourPrice ghp = goldenHourPriceRepository.findById(priceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khung giờ vàng với ID: " + priceId));

        if (!ghp.getPitch().getId().equals(pitch.getId())) {
            throw new BadRequestException("Khung giờ vàng ID " + priceId + " không thuộc về sân bóng ID " + pitchId);
        }

        pitch.removeGoldenHourPrice(ghp);
        goldenHourPriceRepository.delete(ghp);
    }

    @Override
    @Transactional(readOnly = true)
    public Pitch findEntityById(Long id) {
        return pitchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân bóng với mã: " + id));
    }
}
