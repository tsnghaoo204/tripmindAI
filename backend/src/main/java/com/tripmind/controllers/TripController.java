package com.tripmind.controllers;

import com.tripmind.configurations.security.SecurityUtils;
import com.tripmind.domains.requests.CreateTripRequest;
import com.tripmind.domains.requests.UpdateTripPhaseRequest;
import com.tripmind.domains.requests.UpdateTripRequest;
import com.tripmind.domains.responses.ApiResponse;
import com.tripmind.domains.responses.TripResponse;
import com.tripmind.enums.TripPhase;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import com.tripmind.services.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    @Operation(summary = "Create a new trip (days are generated automatically)")
    public ResponseEntity<ApiResponse<TripResponse>> createTrip(@Valid @RequestBody CreateTripRequest request) {
        TripResponse trip = tripService.createTrip(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Trip created successfully", trip));
    }

    @GetMapping
    @Operation(summary = "Get trips of current user, optionally filtered by status")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getMyTrips(
            @Parameter(description = "upcoming | ongoing | past")
            @RequestParam(required = false) String status) {
        List<TripResponse> trips = tripService.getTrips(SecurityUtils.getCurrentUserId(), toPhase(status));
        return ResponseEntity.ok(ApiResponse.ok(trips));
    }

    @GetMapping("/{tripId}")
    @Operation(summary = "Get trip details with day summaries")
    public ResponseEntity<ApiResponse<TripResponse>> getTripDetails(@PathVariable Long tripId) {
        return ResponseEntity.ok(ApiResponse.ok(tripService.getTrip(SecurityUtils.getCurrentUserId(), tripId)));
    }

    @PutMapping("/{tripId}")
    @Operation(summary = "Update trip; changing dates adds or removes days at the end")
    public ResponseEntity<ApiResponse<TripResponse>> updateTrip(
            @PathVariable Long tripId,
            @Valid @RequestBody UpdateTripRequest request) {
        TripResponse trip = tripService.updateTrip(SecurityUtils.getCurrentUserId(), tripId, request);
        return ResponseEntity.ok(ApiResponse.ok("Trip updated successfully", trip));
    }

    @PutMapping("/{tripId}/phase")
    @Operation(summary = "Set trip phase manually (BEFORE/DURING/AFTER) or null to derive from dates")
    public ResponseEntity<ApiResponse<TripResponse>> updatePhase(
            @PathVariable Long tripId,
            @RequestBody UpdateTripPhaseRequest request) {
        TripResponse trip = tripService.updatePhase(SecurityUtils.getCurrentUserId(), tripId, request.getPhase());
        return ResponseEntity.ok(ApiResponse.ok("Trip phase updated", trip));
    }

    @DeleteMapping("/{tripId}")
    @Operation(summary = "Delete a trip with its days, activities, expenses and conversations")
    public ResponseEntity<ApiResponse<Void>> deleteTrip(@PathVariable Long tripId) {
        tripService.deleteTrip(SecurityUtils.getCurrentUserId(), tripId);
        return ResponseEntity.ok(ApiResponse.ok("Trip deleted successfully", null));
    }

    private TripPhase toPhase(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return switch (status.trim().toLowerCase()) {
            case "upcoming" -> TripPhase.BEFORE;
            case "ongoing" -> TripPhase.DURING;
            case "past" -> TripPhase.AFTER;
            default -> throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "status must be one of upcoming, ongoing, past");
        };
    }
}
