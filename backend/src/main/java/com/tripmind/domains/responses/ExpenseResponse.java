package com.tripmind.domains.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tripmind.enums.ExpenseCategory;
import com.tripmind.enums.ExpenseSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExpenseResponse {

    private Long id;
    private Long tripId;
    private Long activityId;
    private String activityTitle;
    private ExpenseCategory category;
    private Long amount;
    private String currency;
    private String description;
    private LocalDate expenseDate;
    private ExpenseSource source;
    private Instant createdAt;
}
