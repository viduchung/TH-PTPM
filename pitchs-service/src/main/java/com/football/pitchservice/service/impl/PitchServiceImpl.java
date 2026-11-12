package com.football.pitchservice.service.impl;

import com.football.pitchservice.dto.PitchRequestDTO;
import com.football.pitchservice.dto.PitchResponseDTO;
import com.football.pitchservice.dto.PriceResponseDTO;
import com.football.pitchservice.entity.Pitch;
import com.football.pitchservice.exception.ResourceNotFoundException;
import com.football.pitchservice.repository.PitchRepository;
import com.football.pitchservice.service.PitchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PitchServiceImpl implements PitchService {

    private final PitchRepository pitchRepository;

    public PitchServiceImpl(PitchRepository pitchRepository) {
        this.pitchRepository = pitchRepository;
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
    @Transactional(readOnly = true)
    public Pitch findEntityById(Long id) {
        return pitchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân bóng với mã: " + id));
    }
}
