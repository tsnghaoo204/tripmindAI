package com.tripmind.services;

import com.tripmind.domains.responses.BudgetSummaryResponse;

public interface BudgetService {

    BudgetSummaryResponse getTripBudgetSummary(Long userId, Long tripId);
}
