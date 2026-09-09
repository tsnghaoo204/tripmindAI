package com.tripmind.controllers;

import com.tripmind.domains.requests.CreateActivityRequest;
import com.tripmind.domains.requests.ReorderActivitiesRequest;
import com.tripmind.domains.requests.UpdateActivityRequest;
import com.tripmind.domains.responses.ActivityResponse;
import com.tripmind.domains.responses.ApiResponse;
import com.tripmind.domains.responses.ItineraryDayResponse;
import com.tripmind.domains.responses.ItineraryResponse;
import com.tripmind.services.ItineraryService;
import com.tripmind.configurations.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Itinerary & Activities", description = "APIs for trip itineraries, activities, distance calculations and reordering")
public class ItineraryController {

    private final ItineraryService itineraryService;

    @GetMapping("/api/trips/{tripId}/itinerary")
    @Operation(summary = "Get full trip itinerary with calculated distances and ideal timing tips")
    public ResponseEntity<ApiResponse<ItineraryResponse>> getTripItinerary(@PathVariable Long tripId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ItineraryResponse response = itineraryService.getItinerary(currentUserId, tripId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/api/trips/{tripId}/itinerary/activities")
    @Operation(summary = "Add an activity to a specific day in the trip itinerary")
    public ResponseEntity<ApiResponse<ActivityResponse>> addActivity(
            @PathVariable Long tripId,
            @Valid @RequestBody CreateActivityRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ActivityResponse response = itineraryService.addActivity(currentUserId, tripId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Activity added successfully", response));
    }

    @PutMapping("/api/activities/{activityId}")
    @Operation(summary = "Update activity details (notes, cost, timing, status)")
    public ResponseEntity<ApiResponse<ActivityResponse>> updateActivity(
            @PathVariable Long activityId,
            @Valid @RequestBody UpdateActivityRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ActivityResponse response = itineraryService.updateActivity(currentUserId, activityId, request);
        return ResponseEntity.ok(ApiResponse.ok("Activity updated successfully", response));
    }

    @DeleteMapping("/api/activities/{activityId}")
    @Operation(summary = "Delete an activity and re-index remaining activities")
    public ResponseEntity<ApiResponse<Void>> deleteActivity(@PathVariable Long activityId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        itineraryService.deleteActivity(currentUserId, activityId);
        return ResponseEntity.ok(ApiResponse.ok("Activity deleted successfully", null));
    }

    @PutMapping("/api/itinerary-days/{dayId}/reorder")
    @Operation(summary = "Reorder activities for a day (drag & drop) and recalculate distances")
    public ResponseEntity<ApiResponse<ItineraryDayResponse>> reorderActivities(
            @PathVariable Long dayId,
            @Valid @RequestBody ReorderActivitiesRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ItineraryDayResponse response = itineraryService.reorderActivities(currentUserId, dayId, request.getActivityIds());
        return ResponseEntity.ok(ApiResponse.ok("Activities reordered successfully", response));
    }
}
