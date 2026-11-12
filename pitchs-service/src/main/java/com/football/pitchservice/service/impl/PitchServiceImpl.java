package com.football.pitchservice.service.impl;

import com.football.pitchservice.dto.*;
import com.football.pitchservice.entity.GoldenHourPrice;
import com.football.pitchservice.entity.Pitch;
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
            throw new IllegalArgumentException("Khung giờ vàng ID " + priceId + " không thuộc về sân bóng ID " + pitchId);
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
