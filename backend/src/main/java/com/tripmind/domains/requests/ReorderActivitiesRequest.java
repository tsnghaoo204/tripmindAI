package com.tripmind.domains.requests;

import jakarta.validation.constraints.NotEmpty;
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
public class ReorderActivitiesRequest {

    @NotEmpty(message = "Activity IDs list must not be empty")
    @Size(max = 200, message = "At most 200 activities")
    private List<Long> activityIds;
}
