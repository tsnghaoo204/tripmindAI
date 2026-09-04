package com.tripmind.domains.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetSummaryResponse {

    private Long budget;
    private String currency;
    private Long estimatedTotal;
    private Long actualTotal;
    private Long remaining;
    private Map<String, Long> byCategory;
    private String warningLevel; // NONE, NEAR_LIMIT (>90%), OVER (>100%)
}
