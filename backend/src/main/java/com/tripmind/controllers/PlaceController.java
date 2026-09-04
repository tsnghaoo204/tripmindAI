package com.tripmind.controllers;

import com.tripmind.domains.responses.ApiResponse;
import com.tripmind.domains.responses.PlaceResponse;
import com.tripmind.services.PlaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
@Tag(name = "Places", description = "Places catalog, search, and Google Maps reviews APIs")
public class PlaceController {

    private final PlaceService placeService;

    @GetMapping("/search")
    @Operation(summary = "Search places by keyword & destination")
    public ResponseEntity<ApiResponse<List<PlaceResponse>>> searchPlaces(
            @RequestParam String query,
            @RequestParam(required = false) String destination) {
        List<PlaceResponse> places = placeService.searchPlaces(query, destination);
        return ResponseEntity.ok(ApiResponse.ok(places));
    }

    @GetMapping("/recommendations/nearby")
    @Operation(summary = "Get contextual next places recommended around current location")
    public ResponseEntity<ApiResponse<List<PlaceResponse>>> getNearbyRecommendations(
            @RequestParam BigDecimal lat,
            @RequestParam BigDecimal lng,
            @RequestParam(defaultValue = "3000") Integer radius,
            @RequestParam(required = false) String category) {
        List<PlaceResponse> places = placeService.getNearbyRecommendations(lat, lng, radius, category);
        return ResponseEntity.ok(ApiResponse.ok(places));
    }

    @GetMapping("/{provider}/{externalId}")
    @Operation(summary = "Get place details with Google Maps rating and reviews")
    public ResponseEntity<ApiResponse<PlaceResponse>> getPlaceDetails(
            @PathVariable String provider,
            @PathVariable String externalId) {
        PlaceResponse place = placeService.getPlaceDetails(provider, externalId);
        return ResponseEntity.ok(ApiResponse.ok(place));
    }
}
