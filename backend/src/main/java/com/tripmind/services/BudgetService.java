package com.tripmind.services;

import com.tripmind.domains.responses.BudgetSummaryResponse;
import com.tripmind.entities.TripEntity;

public interface BudgetService {

    BudgetSummaryResponse getTripBudgetSummary(Long userId, Long tripId);

    /** Cho các dịch vụ đã kiểm sở hữu chuyến (công cụ AI). */
    BudgetSummaryResponse summarize(TripEntity trip);
}
