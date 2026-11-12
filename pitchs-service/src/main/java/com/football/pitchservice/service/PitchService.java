package com.football.pitchservice.service;

import com.football.pitchservice.entity.Pitch;

import java.util.List;

public interface PitchService {
    List<Pitch> getAllPitches();
    Pitch getPitchById(Long id);
    Pitch createPitch(Pitch pitch);
    Pitch updatePitch(Long id, Pitch pitchDetails);
    void deletePitch(Long id);
}

