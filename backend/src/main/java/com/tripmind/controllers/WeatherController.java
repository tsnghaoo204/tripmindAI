package com.tripmind.controllers;

import com.tripmind.configurations.security.SecurityUtils;
import com.tripmind.domains.responses.ApiResponse;
import com.tripmind.domains.responses.TripWeatherResponse;
import com.tripmind.services.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Weather", description = "Daily weather of a trip (forecast or climate normal, always labelled)")
@SecurityRequirement(name = "bearerAuth")
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping("/api/trips/{tripId}/weather")
    @Operation(summary = "Weather for each day of the trip; empty days with a reason when Open-Meteo is down")
    public ResponseEntity<ApiResponse<TripWeatherResponse>> getWeather(@PathVariable Long tripId) {
        return ResponseEntity.ok(ApiResponse.ok(weatherService.getTripWeather(SecurityUtils.getCurrentUserId(), tripId)));
    }
}
