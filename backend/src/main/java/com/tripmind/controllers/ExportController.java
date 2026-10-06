package com.tripmind.controllers;

import com.tripmind.configurations.security.SecurityUtils;
import com.tripmind.domains.responses.ItineraryResponse;
import com.tripmind.entities.TripEntity;
import com.tripmind.services.IcsExporter;
import com.tripmind.services.ItineraryService;
import com.tripmind.services.TripClock;
import com.tripmind.services.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

@RestController
@RequiredArgsConstructor
@Tag(name = "Export", description = "Export the itinerary to calendar apps")
@SecurityRequirement(name = "bearerAuth")
public class ExportController {

    private static final MediaType TEXT_CALENDAR = new MediaType("text", "calendar", StandardCharsets.UTF_8);

    private final TripService tripService;
    private final ItineraryService itineraryService;
    private final TripClock tripClock;
    private final IcsExporter icsExporter;

    @GetMapping("/api/trips/{tripId}/export.ics")
    @Operation(summary = "Download the itinerary as an iCalendar file (Google Calendar, Apple Calendar...)")
    public ResponseEntity<byte[]> exportIcs(@PathVariable Long tripId) {
        Long userId = SecurityUtils.getCurrentUserId();
        TripEntity trip = tripService.getOwnedTrip(userId, tripId);
        ItineraryResponse itinerary = itineraryService.getItinerary(userId, tripId);
        byte[] body = icsExporter.export(itinerary, tripClock.zoneOf(trip)).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(TEXT_CALENDAR)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(slug(trip.getName()) + ".ics", StandardCharsets.UTF_8)
                        .build().toString())
                .body(body);
    }

    private static String slug(String name) {
        String plain = Normalizer.normalize(name.replace('đ', 'd').replace('Đ', 'D'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("(^-|-$)", "")
                .toLowerCase();
        return plain.isEmpty() ? "tripmind" : plain;
    }
}
