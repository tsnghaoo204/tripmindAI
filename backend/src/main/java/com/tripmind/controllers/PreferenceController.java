package com.tripmind.controllers;

import com.tripmind.configurations.security.SecurityUtils;
import com.tripmind.domains.requests.UserPreferencesRequest;
import com.tripmind.domains.responses.ApiResponse;
import com.tripmind.domains.responses.UserPreferencesResponse;
import com.tripmind.services.UserPreferencesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me/preferences")
@RequiredArgsConstructor
@Tag(name = "Preferences", description = "Default travel preferences used when creating new trips")
@SecurityRequirement(name = "bearerAuth")
public class PreferenceController {

    private final UserPreferencesService userPreferencesService;

    @GetMapping
    @Operation(summary = "Get default travel preferences of current user")
    public ResponseEntity<ApiResponse<UserPreferencesResponse>> get() {
        return ResponseEntity.ok(ApiResponse.ok(userPreferencesService.get(SecurityUtils.getCurrentUserId())));
    }

    @PutMapping
    @Operation(summary = "Replace default travel preferences of current user")
    public ResponseEntity<ApiResponse<UserPreferencesResponse>> update(@Valid @RequestBody UserPreferencesRequest request) {
        UserPreferencesResponse response = userPreferencesService.update(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Preferences updated", response));
    }
}
