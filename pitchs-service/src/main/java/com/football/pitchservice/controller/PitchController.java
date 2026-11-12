package com.football.pitchservice.controller;

import com.football.pitchservice.dto.*;
import com.football.pitchservice.service.PitchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/pitches", "/api/pitchs", "/api/v1/pitches"})
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
@Tag(name = "Pitch Inter-Service Controller (TH1 - TH7)", description = "RESTful APIs quản lý sân bóng, bảo mật JWT RBAC, Provider Báo giá /quote và Service Discovery")
public class PitchController {

    private final PitchService pitchService;
    private final Environment environment;

    @Value("${server.port:8082}")
    private String configuredPort;

    public PitchController(PitchService pitchService, Environment environment) {
        this.pitchService = pitchService;
        this.environment = environment;
    }

    // ============================================================
    // TH7: KIỂM THỬ INSTANCE & CÂN BẰNG TẢI (LOAD BALANCING)
    // ============================================================
    @Operation(summary = "Kiểm tra port và instance đang phục vụ request (TH7 Load Balancing)")
    @GetMapping("/instance-info")
    public ResponseEntity<Map<String, Object>> getInstanceInfo() {
        String activePort = environment.getProperty("local.server.port", configuredPort);
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("serviceName", "pitchs-service");
        info.put("activePort", activePort);
        info.put("status", "UP");
        info.put("message", "Request được phục vụ bởi pitchs-service instance tại port: " + activePort);
        return ResponseEntity.ok(info);
    }

    // ============================================================
    // CÁC ENDPOINT PUBLIC (AI CŨNG XEM ĐƯỢC / KHÔNG CẦN TOKEN)
    // ============================================================

    @Operation(summary = "Tìm kiếm & lọc sân theo loại, trạng thái kết hợp phân trang (TH3)")
    @GetMapping("/search")
    public ResponseEntity<PageResponseDTO<PitchResponseDTO>> searchPitches(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        return ResponseEntity.ok(pitchService.searchPitches(type, status, page, size, sortBy, sortDir));
    }

    @Operation(summary = "Lấy danh sách tất cả các sân bóng")
    @GetMapping
    public ResponseEntity<List<PitchResponseDTO>> getAllPitches() {
        return ResponseEntity.ok(pitchService.getAllPitches());
    }

    @Operation(summary = "Lấy thông tin chi tiết một sân bóng theo ID")
    @GetMapping("/{id}")
    public ResponseEntity<PitchResponseDTO> getPitchById(@PathVariable Long id) {
        return ResponseEntity.ok(pitchService.getPitchById(id));
    }

    @Operation(summary = "Lấy thông tin báo giá cơ bản của sân (cho booking-service)")
    @GetMapping("/{id}/price")
    public ResponseEntity<PriceResponseDTO> getPitchPrice(@PathVariable Long id) {
        return ResponseEntity.ok(pitchService.getPitchPrice(id));
    }

    // ============================================================
    // TH6: PROVIDER API /quote - TÍNH TOÁN BÁO GIÁ DỰA TRÊN THỜI GIAN
    // ============================================================
    @Operation(summary = "Tính toán báo giá thuê sân theo khung thời gian & giờ vàng (TH6 Provider)")
    @GetMapping({"/{id}/quote", "/{id}/quotes"})
    public ResponseEntity<QuoteResponseDTO> getPitchQuote(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime
    ) {
        return ResponseEntity.ok(pitchService.calculateQuote(id, date, startTime, endTime));
    }

    // ============================================================
    // CÁC ENDPOINT BẢO VỆ (TH5: BẮT BUỘC ROLE OWNER HOẶC ADMIN)
    // ============================================================

    @Operation(summary = "Thêm mới một sân bóng (Yêu cầu quyền OWNER hoặc ADMIN)")
    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'FIELD_OWNER', 'ADMIN', 'INTERNAL_SERVICE')")
    public ResponseEntity<PitchResponseDTO> createPitch(@Valid @RequestBody PitchRequestDTO request) {
        PitchResponseDTO created = pitchService.createPitch(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @Operation(summary = "Cập nhật thông tin sân bóng theo ID (Yêu cầu quyền OWNER hoặc ADMIN)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'FIELD_OWNER', 'ADMIN', 'INTERNAL_SERVICE')")
    public ResponseEntity<PitchResponseDTO> updatePitch(
            @PathVariable Long id,
            @Valid @RequestBody PitchRequestDTO request
    ) {
        return ResponseEntity.ok(pitchService.updatePitch(id, request));
    }

    @Operation(summary = "Xóa sân bóng theo ID (Yêu cầu quyền OWNER hoặc ADMIN)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'FIELD_OWNER', 'ADMIN', 'INTERNAL_SERVICE')")
    public ResponseEntity<String> deletePitch(@PathVariable Long id) {
        pitchService.deletePitch(id);
        return ResponseEntity.ok("Xóa thành công sân bóng có ID: " + id);
    }

    @Operation(summary = "Thêm khung giá giờ vàng cho sân bóng (Yêu cầu quyền OWNER hoặc ADMIN)")
    @PostMapping("/{id}/golden-hours")
    @PreAuthorize("hasAnyRole('OWNER', 'FIELD_OWNER', 'ADMIN', 'INTERNAL_SERVICE')")
    public ResponseEntity<GoldenHourPriceResponseDTO> addGoldenHourPrice(
            @PathVariable Long id,
            @Valid @RequestBody GoldenHourPriceRequestDTO request
    ) {
        GoldenHourPriceResponseDTO created = pitchService.addGoldenHourPrice(id, request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @Operation(summary = "Xóa khung giá giờ vàng của sân bóng (Yêu cầu quyền OWNER hoặc ADMIN)")
    @DeleteMapping("/{id}/golden-hours/{priceId}")
    @PreAuthorize("hasAnyRole('OWNER', 'FIELD_OWNER', 'ADMIN', 'INTERNAL_SERVICE')")
    public ResponseEntity<String> deleteGoldenHourPrice(
            @PathVariable Long id,
            @PathVariable Long priceId
    ) {
        pitchService.deleteGoldenHourPrice(id, priceId);
        return ResponseEntity.ok("Xóa thành công khung giá giờ vàng ID: " + priceId + " của sân ID: " + id);
    }
}
