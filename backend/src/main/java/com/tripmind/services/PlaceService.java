package com.tripmind.services;

import com.tripmind.domains.responses.PlaceResponse;

import java.math.BigDecimal;
import java.util.List;

public interface PlaceService {

    List<PlaceResponse> searchPlaces(String query, String destinationName);

    List<PlaceResponse> getNearbyRecommendations(BigDecimal latitude, BigDecimal longitude, Integer radiusMeters, String category);

    PlaceResponse getPlaceDetails(String provider, String externalId);
}
