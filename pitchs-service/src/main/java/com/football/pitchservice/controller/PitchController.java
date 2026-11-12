package com.football.pitchservice.controller;

import com.football.pitchservice.entity.Pitch;
import com.football.pitchservice.service.PitchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/pitches", "/api/v1/pitches"})
@CrossOrigin(origins = "*")
@Tag(name = "Pitch Controller", description = "RESTful APIs quản lý sân bóng (CRUD)")
public class PitchController {

    private final PitchService pitchService;

    public PitchController(PitchService pitchService) {
        this.pitchService = pitchService;
    }

    @Operation(summary = "Lấy danh sách tất cả các sân bóng")
    @GetMapping
    public ResponseEntity<List<Pitch>> getAllPitches() {
        return ResponseEntity.ok(pitchService.getAllPitches());
    }

    @Operation(summary = "Lấy thông tin chi tiết một sân bóng theo ID")
    @GetMapping("/{id}")
    public ResponseEntity<Pitch> getPitchById(@PathVariable Long id) {
        return ResponseEntity.ok(pitchService.getPitchById(id));
    }

    @Operation(summary = "Thêm mới một sân bóng")
    @PostMapping
    public ResponseEntity<Pitch> createPitch(@RequestBody Pitch pitch) {
        Pitch createdPitch = pitchService.createPitch(pitch);
        return new ResponseEntity<>(createdPitch, HttpStatus.CREATED);
    }

    @Operation(summary = "Cập nhật thông tin sân bóng theo ID")
    @PutMapping("/{id}")
    public ResponseEntity<Pitch> updatePitch(@PathVariable Long id, @RequestBody Pitch pitch) {
        return ResponseEntity.ok(pitchService.updatePitch(id, pitch));
    }

    @Operation(summary = "Xóa sân bóng theo ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePitch(@PathVariable Long id) {
        pitchService.deletePitch(id);
        return ResponseEntity.ok("Xóa thành công sân bóng có ID: " + id);
    }
}

