package com.football.data.pitch.entity;

/**
 * Loại sân bóng đá
 */
public enum PitchType {
    SAN_5("Sân 5 người"),
    SAN_7("Sân 7 người"),
    SAN_11("Sân 11 người");

    private final String description;

    PitchType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
