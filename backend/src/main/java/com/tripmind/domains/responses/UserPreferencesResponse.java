package com.tripmind.domains.responses;

import com.tripmind.enums.BudgetPreference;
import com.tripmind.enums.TravelStyle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesResponse {

    private TravelStyle travelStyle;
    private BudgetPreference budgetPreference;
    private List<String> preferences;
}
