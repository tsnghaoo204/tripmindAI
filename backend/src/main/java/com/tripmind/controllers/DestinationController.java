package com.tripmind.controllers;

import com.tripmind.domains.responses.ApiResponse;
import com.tripmind.domains.responses.DestinationResponse;
import com.tripmind.services.DestinationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/destinations")
@RequiredArgsConstructor
@Tag(name = "Destinations", description = "Destination catalog and global search APIs")
public class DestinationController {

    private final DestinationService destinationService;

    @GetMapping
    @Operation(summary = "Get popular destinations or search destinations by keyword")
    public ResponseEntity<ApiResponse<List<DestinationResponse>>> getDestinations(
            @RequestParam(required = false) String query) {
        List<DestinationResponse> destinations = destinationService.searchDestinations(query);
        return ResponseEntity.ok(ApiResponse.ok(destinations));
    }

    @GetMapping("/popular")
    @Operation(summary = "Get popular destination catalog")
    public ResponseEntity<ApiResponse<List<DestinationResponse>>> getPopularDestinations() {
        List<DestinationResponse> destinations = destinationService.getPopularDestinations();
        return ResponseEntity.ok(ApiResponse.ok(destinations));
    }

    @GetMapping("/{destinationId}")
    @Operation(summary = "Get destination details by ID")
    public ResponseEntity<ApiResponse<DestinationResponse>> getDestinationById(
            @PathVariable Long destinationId) {
        DestinationResponse destination = destinationService.getDestinationById(destinationId);
        return ResponseEntity.ok(ApiResponse.ok(destination));
    }
}
