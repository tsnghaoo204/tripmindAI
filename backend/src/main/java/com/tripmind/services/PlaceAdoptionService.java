package com.tripmind.services;

import com.tripmind.entities.PlaceEntity;
import com.tripmind.enums.PlaceAdoption;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import com.tripmind.repositories.PlaceRepository;
import com.tripmind.services.clients.GooglePlacesClient;
import com.tripmind.services.clients.GooglePlacesClient.GooglePlace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Ghi một địa điểm từ nhà cung cấp ngoài vào bảng {@code places} — chỉ khi có hành động của
 * người dùng (lưu, thêm vào lịch trình, duyệt đề xuất, sinh lịch trình). Một địa điểm chỉ có
 * một dòng theo {@code (provider, external_id)} (DI-8); {@code adopted_via} giữ lý do đầu tiên.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceAdoptionService {

    public static final String PROVIDER_GOOGLE = "GOOGLE";

    private final PlaceRepository placeRepository;
    private final GooglePlacesClient googlePlacesClient;
    private final PlaceCandidateCache candidateCache;

    /**
     * Địa điểm theo {@code placeId} (đã có trong CSDL) hoặc theo cặp mã nhà cung cấp. Cả hai
     * đều null thì trả {@code null}: hoạt động không gắn địa điểm là hợp lệ (DI-4).
     */
    @Transactional
    public PlaceEntity resolve(Long placeId, String provider, String externalId, PlaceAdoption via) {
        if (placeId != null) {
            return placeRepository.findById(placeId)
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Place not found with id: " + placeId));
        }
        if (externalId != null && !externalId.isBlank()) {
            return adopt(provider == null || provider.isBlank() ? PROVIDER_GOOGLE : provider, externalId, via);
        }
        return null;
    }

    @Transactional
    public PlaceEntity adopt(String provider, String externalId, PlaceAdoption via) {
        String normalizedProvider = provider.toUpperCase();
        Optional<PlaceEntity> existing = placeRepository.findByProviderAndExternalId(normalizedProvider, externalId);
        if (existing.isPresent()) {
            return existing.get();
        }
        if (!PROVIDER_GOOGLE.equals(normalizedProvider)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Place " + normalizedProvider + "/" + externalId + " not found");
        }
        GooglePlace place = findCandidate(externalId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Google place " + externalId + " not found"));
        return save(fromGoogle(place, via));
    }

    /** Có trong bộ đệm tìm kiếm gần đây hoặc tra được từ Google. Không ghi gì. */
    public Optional<GooglePlace> findCandidate(String externalId) {
        Optional<GooglePlace> cached = candidateCache.get(PROVIDER_GOOGLE, externalId);
        if (cached.isPresent()) {
            return cached;
        }
        try {
            return googlePlacesClient.getPlaceDetails(externalId).filter(GooglePlace::isResolvable);
        } catch (AppException e) {
            log.warn("Khong tra duoc Google place {}: {}", externalId, e.getMessage());
            return Optional.empty();
        }
    }

    public static PlaceEntity fromGoogle(GooglePlace place, PlaceAdoption via) {
        String category = place.types() == null || place.types().isEmpty() ? null : place.types().get(0);
        return PlaceEntity.builder()
                .provider(PROVIDER_GOOGLE)
                .externalId(place.placeId())
                .name(truncate(place.name(), 255))
                .category(truncate(category, 64))
                .latitude(place.latitude())
                .longitude(place.longitude())
                .rating(place.rating())
                .userRatingsTotal(place.userRatingCount() == null ? 0 : place.userRatingCount())
                .priceLevel(place.priceLevel() == null ? null : place.priceLevel().shortValue())
                .address(place.formattedAddress())
                .phoneNumber(truncate(place.phoneNumber(), 32))
                .websiteUrl(place.websiteUri())
                .openingHours(place.openingHours())
                .photoUrls(place.photos())
                .adoptedVia(via)
                .adoptedAt(Instant.now())
                .fetchedAt(Instant.now())
                .build();
    }

    /**
     * Hai yêu cầu nạp cùng một địa điểm cùng lúc: yêu cầu thua vấp {@code UNIQUE (provider,
     * external_id)} và nhận {@code 409}; bấm lại thì đọc được dòng của yêu cầu thắng.
     */
    private PlaceEntity save(PlaceEntity place) {
        PlaceEntity saved = placeRepository.saveAndFlush(place);
        log.info("Adopted place {} '{}' via {}", saved.getExternalId(), saved.getName(), saved.getAdoptedVia());
        return saved;
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
