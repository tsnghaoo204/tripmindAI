package com.tripmind.domains.requests;

import com.tripmind.enums.ActivityType;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateActivityRequest {

    private String title;
    private ActivityType activityType;
    private Long placeId;
    private LocalTime startTime;
    private LocalTime endTime;

    @Min(value = 0, message = "Estimated cost must not be negative")
    private Long estimatedCost;

    private String transportationMode;
    private String notes;
    private String status;
    private String skipReason;
    private Short orderIndex;
}
