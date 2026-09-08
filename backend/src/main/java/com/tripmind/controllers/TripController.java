package com.tripmind.controllers;

import com.tripmind.configurations.security.SecurityUtils;
import com.tripmind.domains.requests.CreateTripRequest;
import com.tripmind.domains.responses.ApiResponse;
import com.tripmind.entities.TripEntity;
import com.tripmind.services.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
@Tag(name = "Trips", description = "Trip planning and management APIs")
@SecurityRequirement(name = "bearerAuth")
public class TripController {

    private final TripService tripService;

    @PostMapping
    @Operation(summary = "Create a new trip")
    public ResponseEntity<ApiResponse<TripEntity>> createTrip(
            @Valid @RequestBody CreateTripRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        TripEntity trip = tripService.createTrip(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Trip created successfully", trip));
    }

    @GetMapping
    @Operation(summary = "Get all trips for current user")
    public ResponseEntity<ApiResponse<List<TripEntity>>> getMyTrips() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<TripEntity> trips = tripService.getTripsByUser(currentUserId);
        return ResponseEntity.ok(ApiResponse.ok(trips));
    }

    @GetMapping("/{tripId}")
    @Operation(summary = "Get detailed trip itinerary by ID")
    public ResponseEntity<ApiResponse<TripEntity>> getTripDetails(@PathVariable Long tripId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        TripEntity trip = tripService.getTripById(currentUserId, tripId);
        return ResponseEntity.ok(ApiResponse.ok(trip));
    }

    @DeleteMapping("/{tripId}")
    @Operation(summary = "Delete a trip")
    public ResponseEntity<ApiResponse<Void>> deleteTrip(@PathVariable Long tripId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        tripService.deleteTrip(currentUserId, tripId);
        return ResponseEntity.ok(ApiResponse.ok("Trip deleted successfully", null));
    }
}
