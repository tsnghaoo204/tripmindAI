package com.tripmind.controllers;

import com.tripmind.configurations.security.SecurityUtils;
import com.tripmind.domains.requests.ChecklistItemRequest;
import com.tripmind.domains.requests.UpdateChecklistItemRequest;
import com.tripmind.domains.responses.ApiResponse;
import com.tripmind.domains.responses.ChecklistItemResponse;
import com.tripmind.domains.responses.ChecklistSuggestionsResponse;
import com.tripmind.enums.ChecklistKind;
import com.tripmind.services.ChecklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "Checklist", description = "What to pack and what to do before the trip, with rule-based suggestions")
@SecurityRequirement(name = "bearerAuth")
public class ChecklistController {

    private final ChecklistService checklistService;

    @GetMapping("/api/trips/{tripId}/checklist")
    @Operation(summary = "Checklist of a trip, optionally only PACK or TODO")
    public ResponseEntity<ApiResponse<List<ChecklistItemResponse>>> list(@PathVariable Long tripId,
                                                                         @RequestParam(required = false) ChecklistKind kind) {
        return ResponseEntity.ok(ApiResponse.ok(checklistService.list(SecurityUtils.getCurrentUserId(), tripId, kind)));
    }

    @PostMapping("/api/trips/{tripId}/checklist")
    @Operation(summary = "Add one or more items (duplicates by title are skipped)")
    public ResponseEntity<ApiResponse<List<ChecklistItemResponse>>> add(
            @PathVariable Long tripId,
            @RequestBody @NotEmpty @Size(max = 50) List<@Valid ChecklistItemRequest> items) {
        List<ChecklistItemResponse> created = checklistService.add(SecurityUtils.getCurrentUserId(), tripId, items);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Checklist updated", created));
    }

    @GetMapping("/api/trips/{tripId}/checklist/suggestions")
    @Operation(summary = "Rule-based suggestions from weather, activities, destination and group; nothing is saved")
    public ResponseEntity<ApiResponse<ChecklistSuggestionsResponse>> suggestions(@PathVariable Long tripId) {
        return ResponseEntity.ok(ApiResponse.ok(checklistService.suggestions(SecurityUtils.getCurrentUserId(), tripId)));
    }

    @PatchMapping("/api/checklist-items/{itemId}")
    @Operation(summary = "Rename, tick or change due date of an item")
    public ResponseEntity<ApiResponse<ChecklistItemResponse>> update(@PathVariable Long itemId,
                                                                     @Valid @RequestBody UpdateChecklistItemRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(checklistService.update(SecurityUtils.getCurrentUserId(), itemId, request)));
    }

    @DeleteMapping("/api/checklist-items/{itemId}")
    @Operation(summary = "Delete an item")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long itemId) {
        checklistService.delete(SecurityUtils.getCurrentUserId(), itemId);
        return ResponseEntity.ok(ApiResponse.ok("Checklist item deleted", null));
    }
}
