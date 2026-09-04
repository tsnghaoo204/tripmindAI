package com.tripmind.domains.requests;

import com.tripmind.enums.BudgetPreference;
import com.tripmind.enums.TravelStyle;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTripRequest {

    @NotNull(message = "Destination ID is required")
    private Long destinationId;

    @NotBlank(message = "Trip name is required")
    private String name;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must not be in the past")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @Min(value = 1, message = "Travelers must be at least 1")
    private int travelers = 1;

    @Min(value = 0, message = "Budget must not be negative")
    private Long budget;

    private String currency = "VND";

    private TravelStyle travelStyle;

    private BudgetPreference budgetPreference;

    private List<String> preferences;
}
