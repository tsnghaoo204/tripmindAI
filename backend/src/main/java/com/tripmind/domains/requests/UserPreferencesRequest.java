package com.tripmind.domains.requests;

import com.tripmind.enums.BudgetPreference;
import com.tripmind.enums.TravelStyle;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesRequest {

    private TravelStyle travelStyle;

    private BudgetPreference budgetPreference;

    @Size(max = 20, message = "At most 20 preferences")
    private List<@Size(max = 40) String> preferences;
}
