package com.tripmind.controllers;

import com.tripmind.domains.responses.ApiResponse;
import com.tripmind.domains.responses.SavedPlaceResponse;
import com.tripmind.services.SavedPlaceService;
import com.tripmind.configurations.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Saved Places", description = "APIs for bookmarking and managing user's favorite places")
public class SavedPlaceController {

    private final SavedPlaceService savedPlaceService;

    @PostMapping("/api/places/{placeId}/save")
    @Operation(summary = "Save a place to user's favorites")
    public ResponseEntity<ApiResponse<SavedPlaceResponse>> savePlace(@PathVariable Long placeId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        SavedPlaceResponse response = savedPlaceService.savePlace(currentUserId, placeId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Place saved to favorites successfully", response));
    }

    @DeleteMapping("/api/places/{placeId}/save")
    @Operation(summary = "Remove a place from user's favorites")
    public ResponseEntity<ApiResponse<Void>> unsavePlace(@PathVariable Long placeId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        savedPlaceService.unsavePlace(currentUserId, placeId);
        return ResponseEntity.ok(ApiResponse.ok("Place removed from favorites", null));
    }

    @GetMapping("/api/me/saved-places")
    @Operation(summary = "Get all saved favorite places of current user")
    public ResponseEntity<ApiResponse<List<SavedPlaceResponse>>> getMySavedPlaces() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<SavedPlaceResponse> places = savedPlaceService.getSavedPlaces(currentUserId);
        return ResponseEntity.ok(ApiResponse.ok(places));
    }
}
