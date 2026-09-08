package com.tripmind.services;

import com.tripmind.domains.responses.PlaceResponse;
import com.tripmind.entities.PlaceEntity;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import com.tripmind.repositories.PlaceRepository;
import com.tripmind.services.clients.GooglePlacesClient;
import com.tripmind.services.clients.GooglePlacesClient.GooglePlace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceServiceImpl implements PlaceService {

    private final PlaceRepository placeRepository;
    private final GooglePlacesClient googlePlacesClient;

    @Override
    @Transactional
    public List<PlaceResponse> searchPlaces(String query, String destinationName) {
        String searchQuery = (destinationName != null && !destinationName.isBlank())
                ? query + " " + destinationName
                : query;

        List<GooglePlace> googlePlaces = googlePlacesClient.searchText(searchQuery, null);
        List<PlaceResponse> responses = new ArrayList<>();

        for (GooglePlace gp : googlePlaces) {
            responses.add(toPlaceResponse(gp));
        }
        return responses;
    }

    @Override
    @Transactional
    public List<PlaceResponse> getNearbyRecommendations(BigDecimal latitude, BigDecimal longitude, Integer radiusMeters, String category) {
        String query = (category != null && !category.isBlank()) ? category : "attractions";
        List<GooglePlace> googlePlaces = googlePlacesClient.searchText(query, null);
        return googlePlaces.stream().map(this::toPlaceResponse).toList();
    }

    @Override
    @Transactional
    public PlaceResponse getPlaceDetails(String provider, String externalId) {
        return placeRepository.findByProviderAndExternalId(provider, externalId)
                .map(this::fromEntity)
                .orElseGet(() -> {
                    if ("GOOGLE".equalsIgnoreCase(provider)) {
                        return googlePlacesClient.getPlaceDetails(externalId)
                                .map(this::toPlaceResponse)
                                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Place not found: " + externalId));
                    }
                    throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Place not found: " + externalId);
                });
    }

    private PlaceResponse toPlaceResponse(GooglePlace gp) {
        String category = (gp.types() != null && !gp.types().isEmpty()) ? gp.types().get(0) : "GENERAL";
        return PlaceResponse.builder()
                .provider("GOOGLE")
                .externalId(gp.placeId())
                .name(gp.name())
                .category(category)
                .latitude(gp.latitude())
                .longitude(gp.longitude())
                .rating(gp.rating())
                .userRatingsTotal(gp.userRatingCount())
                .priceLevel(gp.priceLevel())
                .address(gp.formattedAddress())
                .phoneNumber(gp.phoneNumber())
                .websiteUrl(gp.websiteUri())
                .openingHours(gp.openingHours())
                .photoUrls(gp.photos())
                .build();
    }

    private PlaceResponse fromEntity(PlaceEntity entity) {
        return PlaceResponse.builder()
                .id(entity.getId())
                .provider(entity.getProvider())
                .externalId(entity.getExternalId())
                .name(entity.getName())
                .category(entity.getCategory())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .rating(entity.getRating())
                .userRatingsTotal(entity.getUserRatingsTotal())
                .priceLevel(entity.getPriceLevel() != null ? entity.getPriceLevel().intValue() : null)
                .address(entity.getAddress())
                .phoneNumber(entity.getPhoneNumber())
                .websiteUrl(entity.getWebsiteUrl())
                .openingHours(entity.getOpeningHours())
                .reviews(entity.getReviews())
                .photoUrls(entity.getPhotoUrls())
                .build();
    }
}
