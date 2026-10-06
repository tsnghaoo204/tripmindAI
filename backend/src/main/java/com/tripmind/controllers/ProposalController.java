package com.tripmind.controllers;

import com.tripmind.configurations.security.SecurityUtils;
import com.tripmind.domains.requests.ApplyProposalRequest;
import com.tripmind.domains.responses.*;
import com.tripmind.services.ai.ExplanationService;
import com.tripmind.services.ai.ItineraryGenerationService;
import com.tripmind.services.ai.ProposalApplyService;
import com.tripmind.services.ai.ProposalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "AI Proposals", description = "Review, apply, reject and undo assistant proposals; explanations; auto-generation")
@SecurityRequirement(name = "bearerAuth")
public class ProposalController {

    private final ProposalService proposalService;
    private final ProposalApplyService applyService;
    private final ExplanationService explanationService;
    private final ItineraryGenerationService generationService;

    @GetMapping("/api/proposals/{proposalId}")
    @Operation(summary = "Proposal details for the before/after diff card")
    public ResponseEntity<ApiResponse<ProposalResponse>> get(@PathVariable Long proposalId) {
        return ResponseEntity.ok(ApiResponse.ok(proposalService.get(SecurityUtils.getCurrentUserId(), proposalId)));
    }

    @PostMapping("/api/trips/{tripId}/ai/apply")
    @Operation(summary = "Apply a pending proposal by id only (the change list is never taken from the client)")
    public ResponseEntity<ApiResponse<ApplyProposalResponse>> apply(@PathVariable Long tripId,
                                                                    @Valid @RequestBody ApplyProposalRequest request) {
        ApplyProposalResponse response = applyService.apply(SecurityUtils.getCurrentUserId(), tripId, request.getProposalId());
        return ResponseEntity.ok(ApiResponse.ok("Proposal applied", response));
    }

    @PostMapping("/api/proposals/{proposalId}/reject")
    @Operation(summary = "Reject a pending proposal (the record is kept)")
    public ResponseEntity<ApiResponse<ProposalResponse>> reject(@PathVariable Long proposalId) {
        return ResponseEntity.ok(ApiResponse.ok("Proposal rejected",
                proposalService.reject(SecurityUtils.getCurrentUserId(), proposalId)));
    }

    @GetMapping("/api/trips/{tripId}/ai/undo/{proposalId}/preview")
    @Operation(summary = "Dry run of undo: exactly what will be deleted, restored, reverted or kept")
    public ResponseEntity<ApiResponse<UndoProposalResponse>> undoPreview(@PathVariable Long tripId,
                                                                         @PathVariable Long proposalId) {
        return ResponseEntity.ok(ApiResponse.ok(applyService.undo(SecurityUtils.getCurrentUserId(), tripId, proposalId, true)));
    }

    @PostMapping("/api/trips/{tripId}/ai/undo")
    @Operation(summary = "Undo an applied proposal within the undo window; manual edits are kept")
    public ResponseEntity<ApiResponse<UndoProposalResponse>> undo(@PathVariable Long tripId,
                                                                  @Valid @RequestBody ApplyProposalRequest request) {
        UndoProposalResponse response = applyService.undo(SecurityUtils.getCurrentUserId(), tripId, request.getProposalId(), false);
        return ResponseEntity.ok(ApiResponse.ok("Proposal reverted", response));
    }

    @GetMapping("/api/activities/{activityId}/explanation")
    @Operation(summary = "Why did the assistant add this activity? Tools run and candidates rejected")
    public ResponseEntity<ApiResponse<ExplanationResponse>> explain(@PathVariable Long activityId) {
        return ResponseEntity.ok(ApiResponse.ok(explanationService.explain(SecurityUtils.getCurrentUserId(), activityId)));
    }

    @PostMapping("/api/trips/{tripId}/ai/generate")
    @Operation(summary = "Generate an itinerary for an empty trip in the background; returns a job id")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generate(@PathVariable Long tripId) {
        Map<String, Object> job = generationService.start(SecurityUtils.getCurrentUserId(), tripId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok("Generation started", job));
    }

    @GetMapping("/api/trips/{tripId}/ai/generate/{jobId}")
    @Operation(summary = "Progress of an itinerary generation job")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generationStatus(@PathVariable Long tripId,
                                                                             @PathVariable String jobId) {
        return ResponseEntity.ok(ApiResponse.ok(generationService.status(SecurityUtils.getCurrentUserId(), tripId, jobId)));
    }
}
