package com.football.pitchservice.controller;

import com.football.pitchservice.dto.*;
import com.football.pitchservice.service.PitchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/pitches", "/api/pitchs", "/api/v1/pitches"})
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
@Tag(name = "Pitch Controller (TH3 & TH4)", description = "RESTful APIs quản lý sân bóng, lọc tìm kiếm phân trang và giá giờ vàng")
public class PitchController {

    private final PitchService pitchService;

    public PitchController(PitchService pitchService) {
        this.pitchService = pitchService;
    }

    // ============================================================
    // TH3: API TÌM KIẾM, LỌC KẾT HỢP PHÂN TRANG (Pageable)
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

    // ============================================================
    // CÁC API CRUD CƠ BẢN VÀ BÁO GIÁ LIÊN SERVICE (TH1, TH2)
    // ============================================================
    @Operation(summary = "Lấy danh sách tất cả các sân bóng")
    @GetMapping
    public ResponseEntity<List<PitchResponseDTO>> getAllPitches() {
        return ResponseEntity.ok(pitchService.getAllPitches());
    }

    @Operation(summary = "Lấy thông tin chi tiết một sân bóng theo ID (bao gồm danh sách giờ vàng)")
    @GetMapping("/{id}")
    public ResponseEntity<PitchResponseDTO> getPitchById(@PathVariable Long id) {
        return ResponseEntity.ok(pitchService.getPitchById(id));
    }

    @Operation(summary = "Lấy thông tin báo giá & tình trạng sân (Chuyên biệt cho booking-service)")
    @GetMapping("/{id}/price")
    public ResponseEntity<PriceResponseDTO> getPitchPrice(@PathVariable Long id) {
        return ResponseEntity.ok(pitchService.getPitchPrice(id));
    }

    @Operation(summary = "Thêm mới một sân bóng (Có validation dữ liệu)")
    @PostMapping
    public ResponseEntity<PitchResponseDTO> createPitch(@Valid @RequestBody PitchRequestDTO request) {
        PitchResponseDTO created = pitchService.createPitch(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @Operation(summary = "Cập nhật thông tin sân bóng theo ID (Có validation dữ liệu)")
    @PutMapping("/{id}")
    public ResponseEntity<PitchResponseDTO> updatePitch(
            @PathVariable Long id,
            @Valid @RequestBody PitchRequestDTO request
    ) {
        return ResponseEntity.ok(pitchService.updatePitch(id, request));
    }

    @Operation(summary = "Xóa sân bóng theo ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePitch(@PathVariable Long id) {
        pitchService.deletePitch(id);
        return ResponseEntity.ok("Xóa thành công sân bóng có ID: " + id);
    }

    // ============================================================
    // TH4: APIS QUẢN LÝ KHUNG GIÁ GIỜ VÀNG (Golden Hour Price)
    // ============================================================
    @Operation(summary = "Thêm khung giá giờ vàng cho sân bóng (TH4)")
    @PostMapping("/{id}/golden-hours")
    public ResponseEntity<GoldenHourPriceResponseDTO> addGoldenHourPrice(
            @PathVariable Long id,
            @Valid @RequestBody GoldenHourPriceRequestDTO request
    ) {
        GoldenHourPriceResponseDTO created = pitchService.addGoldenHourPrice(id, request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @Operation(summary = "Xóa một khung giá giờ vàng của sân bóng (TH4)")
    @DeleteMapping("/{id}/golden-hours/{priceId}")
    public ResponseEntity<String> deleteGoldenHourPrice(
            @PathVariable Long id,
            @PathVariable Long priceId
    ) {
        pitchService.deleteGoldenHourPrice(id, priceId);
        return ResponseEntity.ok("Xóa thành công khung giá giờ vàng ID: " + priceId + " của sân ID: " + id);
    }
}
