package com.tripmind.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripmind.services.clients.GooglePlacesClient.GooglePlace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Bộ đệm ngắn hạn cho kết quả tìm kiếm địa điểm (Redis, mặc định 1 giờ).
 *
 * <p>Tìm kiếm không ghi gì vào bảng {@code places}. Nhưng khi người dùng — hoặc trợ lý —
 * chọn một kết quả vừa tìm, ta cần đúng dữ liệu đó để ghi xuống mà không tốn thêm một lượt
 * Place Details, và cần biết chắc địa điểm đó <b>có thật</b> trong một lượt tìm gần đây
 * (QĐ-08, BR-509). Redis hỏng thì bỏ qua đệm, hệ thống vẫn chạy.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceCandidateCache {

    private static final String KEY_PREFIX = "place:candidate:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Value("${tripmind.google.places.candidate-ttl:PT1H}")
    private Duration ttl;

    public void putAll(String provider, List<GooglePlace> places) {
        for (GooglePlace place : places) {
            if (place.placeId() == null) {
                continue;
            }
            try {
                redis.opsForValue().set(key(provider, place.placeId()), objectMapper.writeValueAsString(place), ttl);
            } catch (Exception e) {
                log.debug("Khong ghi duoc bo dem ung vien {}: {}", place.placeId(), e.getMessage());
                return;
            }
        }
    }

    public Optional<GooglePlace> get(String provider, String externalId) {
        try {
            String json = redis.opsForValue().get(key(provider, externalId));
            return json == null ? Optional.empty() : Optional.of(objectMapper.readValue(json, GooglePlace.class));
        } catch (Exception e) {
            log.debug("Khong doc duoc bo dem ung vien {}: {}", externalId, e.getMessage());
            return Optional.empty();
        }
    }

    private String key(String provider, String externalId) {
        return KEY_PREFIX + provider.toUpperCase() + ":" + externalId;
    }
}
