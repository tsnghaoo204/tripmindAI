package com.tripmind.domains.requests;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderActivitiesRequest {

    @NotEmpty(message = "Activity IDs list must not be empty")
    private List<Long> activityIds;
}
