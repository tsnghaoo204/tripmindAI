package com.tripmind.controllers;

import com.tripmind.domains.responses.ApiResponse;
import com.tripmind.domains.responses.PageResponse;
import com.tripmind.domains.responses.ToolExecutionResponse;
import com.tripmind.enums.ToolExecutionStatus;
import com.tripmind.services.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/** Chỉ đọc, chỉ cho vai trò ADMIN (chặn ở {@code SecurityConfig}: {@code /api/admin/**}). */
@Validated
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Read-only monitoring: tool executions, AI usage, users and trips summary")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/tool-executions")
    @Operation(summary = "Assistant tool executions, filterable by user, tool, status and date range")
    public ResponseEntity<ApiResponse<PageResponse<ToolExecutionResponse>>> toolExecutions(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String tool,
            @RequestParam(required = false) ToolExecutionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.toolExecutions(userId, tool, status, from, to, page, size)));
    }

    @GetMapping("/ai-usage")
    @Operation(summary = "AI usage: turns, tokens, tool calls, error rate, top tools, per day")
    public ResponseEntity<ApiResponse<Map<String, Object>>> aiUsage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.aiUsage(from, to)));
    }

    @GetMapping("/users")
    @Operation(summary = "Users with their trip count")
    public ResponseEntity<ApiResponse<PageResponse<Map<String, Object>>>> users(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.users(page, size)));
    }

    @GetMapping("/trips")
    @Operation(summary = "Trips at summary level (no itinerary content)")
    public ResponseEntity<ApiResponse<PageResponse<Map<String, Object>>>> trips(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.trips(page, size)));
    }
}
