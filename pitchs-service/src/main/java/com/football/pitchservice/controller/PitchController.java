package com.football.pitchservice.controller;

import com.football.pitchservice.dto.PitchRequestDTO;
import com.football.pitchservice.dto.PitchResponseDTO;
import com.football.pitchservice.dto.PriceResponseDTO;
import com.football.pitchservice.service.PitchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
// Hỗ trợ cả 2 định dạng đường dẫn để tránh lỗi typo giữa các thành viên nhóm (/api/pitches và /api/pitchs)
@RequestMapping({"/api/pitches", "/api/pitchs", "/api/v1/pitches"})
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
@Tag(name = "Pitch Inter-Service Controller", description = "APIs quản lý sân bóng và cung cấp dữ liệu cho booking-service (TH2)")
public class PitchController {

    private final PitchService pitchService;

    public PitchController(PitchService pitchService) {
        this.pitchService = pitchService;
    }

    @Operation(summary = "Lấy danh sách tất cả các sân bóng (DTO)")
    @GetMapping
    public ResponseEntity<List<PitchResponseDTO>> getAllPitches() {
        return ResponseEntity.ok(pitchService.getAllPitches());
    }

    @Operation(summary = "Lấy thông tin chi tiết một sân bóng theo ID (Dành cho booking-service)")
    @GetMapping("/{id}")
    public ResponseEntity<PitchResponseDTO> getPitchById(@PathVariable Long id) {
        return ResponseEntity.ok(pitchService.getPitchById(id));
    }

    @Operation(summary = "Lấy thông tin báo giá & tình trạng sân (Chuyên biệt cho booking-service gọi qua WebClient)")
    @GetMapping("/{id}/price")
    public ResponseEntity<PriceResponseDTO> getPitchPrice(@PathVariable Long id) {
        return ResponseEntity.ok(pitchService.getPitchPrice(id));
    }

    @Operation(summary = "Thêm mới một sân bóng")
    @PostMapping
    public ResponseEntity<PitchResponseDTO> createPitch(@Valid @RequestBody PitchRequestDTO request) {
        PitchResponseDTO created = pitchService.createPitch(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @Operation(summary = "Cập nhật thông tin sân bóng theo ID")
    @PutMapping("/{id}")
    public ResponseEntity<PitchResponseDTO> updatePitch(@PathVariable Long id, @Valid @RequestBody PitchRequestDTO request) {
        return ResponseEntity.ok(pitchService.updatePitch(id, request));
    }

    @Operation(summary = "Xóa sân bóng theo ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePitch(@PathVariable Long id) {
        pitchService.deletePitch(id);
        return ResponseEntity.ok("Xóa thành công sân bóng có ID: " + id);
    }
}
