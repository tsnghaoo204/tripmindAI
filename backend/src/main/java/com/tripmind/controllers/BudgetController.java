package com.tripmind.controllers;

import com.tripmind.configurations.security.SecurityUtils;
import com.tripmind.domains.requests.ExpenseRequest;
import com.tripmind.domains.responses.ApiResponse;
import com.tripmind.domains.responses.BudgetSummaryResponse;
import com.tripmind.domains.responses.ExpenseResponse;
import com.tripmind.enums.ExpenseCategory;
import com.tripmind.services.BudgetService;
import com.tripmind.services.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Budget & Expenses", description = "Trip budget summary, daily allowance and actual expenses")
@SecurityRequirement(name = "bearerAuth")
public class BudgetController {

    private final BudgetService budgetService;
    private final ExpenseService expenseService;

    @GetMapping("/api/trips/{tripId}/budget")
    @Operation(summary = "Budget summary: estimated vs actual, warning level, today's allowance")
    public ResponseEntity<ApiResponse<BudgetSummaryResponse>> getBudget(@PathVariable Long tripId) {
        return ResponseEntity.ok(ApiResponse.ok(budgetService.getTripBudgetSummary(SecurityUtils.getCurrentUserId(), tripId)));
    }

    @GetMapping("/api/trips/{tripId}/expenses")
    @Operation(summary = "List actual expenses of a trip, newest first")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> listExpenses(
            @PathVariable Long tripId,
            @RequestParam(required = false) ExpenseCategory category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(
                expenseService.list(SecurityUtils.getCurrentUserId(), tripId, category, from, to)));
    }

    @PostMapping("/api/trips/{tripId}/expenses")
    @Operation(summary = "Record an actual expense (must use the trip currency)")
    public ResponseEntity<ApiResponse<ExpenseResponse>> createExpense(
            @PathVariable Long tripId, @Valid @RequestBody ExpenseRequest request) {
        ExpenseResponse response = expenseService.create(SecurityUtils.getCurrentUserId(), tripId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Expense recorded", response));
    }

    @PutMapping("/api/expenses/{expenseId}")
    @Operation(summary = "Update an expense")
    public ResponseEntity<ApiResponse<ExpenseResponse>> updateExpense(
            @PathVariable Long expenseId, @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Expense updated",
                expenseService.update(SecurityUtils.getCurrentUserId(), expenseId, request)));
    }

    @DeleteMapping("/api/expenses/{expenseId}")
    @Operation(summary = "Delete an expense")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(@PathVariable Long expenseId) {
        expenseService.delete(SecurityUtils.getCurrentUserId(), expenseId);
        return ResponseEntity.ok(ApiResponse.ok("Expense deleted", null));
    }
}
