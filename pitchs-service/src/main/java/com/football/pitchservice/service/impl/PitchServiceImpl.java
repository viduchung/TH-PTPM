package com.football.pitchservice.service.impl;

import com.football.pitchservice.entity.Pitch;
import com.football.pitchservice.repository.PitchRepository;
import com.football.pitchservice.service.PitchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PitchServiceImpl implements PitchService {

    private final PitchRepository pitchRepository;

    public PitchServiceImpl(PitchRepository pitchRepository) {
        this.pitchRepository = pitchRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pitch> getAllPitches() {
        return pitchRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Pitch getPitchById(Long id) {
        return pitchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sân bóng với ID: " + id));
    }

    @Override
    public Pitch createPitch(Pitch pitch) {
        if (pitch.getStatus() == null || pitch.getStatus().isBlank()) {
            pitch.setStatus("AVAILABLE");
        }
        return pitchRepository.save(pitch);
    }

    @Override
    public Pitch updatePitch(Long id, Pitch pitchDetails) {
        Pitch existingPitch = getPitchById(id);

        existingPitch.setName(pitchDetails.getName());
        existingPitch.setType(pitchDetails.getType());
        existingPitch.setPricePerHour(pitchDetails.getPricePerHour());
        existingPitch.setAddress(pitchDetails.getAddress());
        existingPitch.setStatus(pitchDetails.getStatus());
        existingPitch.setDescription(pitchDetails.getDescription());

        return pitchRepository.save(existingPitch);
    }

    @Override
    public void deletePitch(Long id) {
        Pitch existingPitch = getPitchById(id);
        pitchRepository.delete(existingPitch);
    }
}

