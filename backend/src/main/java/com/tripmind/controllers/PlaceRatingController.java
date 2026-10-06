package com.tripmind.controllers;

import com.tripmind.configurations.security.SecurityUtils;
import com.tripmind.domains.requests.PlaceRatingRequest;
import com.tripmind.domains.responses.ApiResponse;
import com.tripmind.domains.responses.PlaceRatingResponse;
import com.tripmind.services.PlaceRatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Place Ratings", description = "Like / dislike places after a trip; the assistant avoids disliked places")
@SecurityRequirement(name = "bearerAuth")
public class PlaceRatingController {

    private final PlaceRatingService ratingService;

    @GetMapping("/api/trips/{tripId}/review/places")
    @Operation(summary = "Places visited in this trip with your current rating")
    public ResponseEntity<ApiResponse<List<PlaceRatingResponse>>> reviewPlaces(@PathVariable Long tripId) {
        return ResponseEntity.ok(ApiResponse.ok(ratingService.reviewPlaces(SecurityUtils.getCurrentUserId(), tripId)));
    }

    @PutMapping("/api/trips/{tripId}/places/{placeId}/rating")
    @Operation(summary = "Rate a place of an ended trip (overwrites your previous rating)")
    public ResponseEntity<ApiResponse<PlaceRatingResponse>> rate(@PathVariable Long tripId, @PathVariable Long placeId,
                                                                 @Valid @RequestBody PlaceRatingRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Place rated",
                ratingService.rate(SecurityUtils.getCurrentUserId(), tripId, placeId, request)));
    }

    @DeleteMapping("/api/trips/{tripId}/places/{placeId}/rating")
    @Operation(summary = "Remove your rating of a place")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long tripId, @PathVariable Long placeId) {
        ratingService.delete(SecurityUtils.getCurrentUserId(), tripId, placeId);
        return ResponseEntity.ok(ApiResponse.ok("Rating removed", null));
    }

    @GetMapping("/api/me/place-ratings")
    @Operation(summary = "All your place ratings, newest first")
    public ResponseEntity<ApiResponse<List<PlaceRatingResponse>>> myRatings() {
        return ResponseEntity.ok(ApiResponse.ok(ratingService.myRatings(SecurityUtils.getCurrentUserId())));
    }
}
