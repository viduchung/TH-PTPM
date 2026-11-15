package com.pitchos.booking.client;

import com.pitchos.booking.dto.PitchResponseDTO;
import com.pitchos.booking.exception.PitchServiceUnavailableException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Cách gọi cũ bằng OpenFeign (PitchClient + CustomErrorDecoder), giữ nguyên để tương thích.
 * Bật bằng pitch.client.type=feign. 404 do CustomErrorDecoder chuyển thành ResourceNotFoundException;
 * mọi FeignException khác (tắt service, timeout, 5xx) → 503.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pitch.client.type", havingValue = "feign")
public class FeignPitchGateway implements PitchGateway {

    private final PitchClient pitchClient;

    @Override
    public PitchResponseDTO getPitch(Long pitchId) {
        try {
            return PitchResponseValidator.requireValid(pitchId, pitchClient.getPitchById(pitchId));
        } catch (FeignException ex) {
            log.warn("Không gọi được Pitch Service (OpenFeign) cho sân {}: {}", pitchId, ex.getMessage());
            throw new PitchServiceUnavailableException(PitchServiceUnavailableException.DEFAULT_MESSAGE, ex);
        }
    }
}
