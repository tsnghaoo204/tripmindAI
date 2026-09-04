package com.tripmind.domains.requests;

import com.tripmind.enums.ActivityType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateActivityRequest {

    @NotNull(message = "Day number is required")
    @Min(value = 1, message = "Day number must be at least 1")
    private Integer dayNumber;

    @NotBlank(message = "Activity title is required")
    private String title;

    private Long placeId;

    @NotNull(message = "Activity type is required")
    private ActivityType activityType;

    private LocalTime startTime;

    private LocalTime endTime;

    @Min(value = 0, message = "Estimated cost must not be negative")
    private Long estimatedCost;

    private String transportationMode;

    private String notes;
}
