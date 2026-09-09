package com.tripmind.domains.responses;

import com.tripmind.enums.ActivityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityResponse {

    private Long id;
    private Long dayId;
    private Short dayNumber;
    private Short orderIndex;
    private String title;
    private ActivityType activityType;
    private String createdBy;
    private LocalTime startTime;
    private LocalTime endTime;
    private Long estimatedCost;
    private String transportationMode;
    private String notes;
    private String status;

    private PlaceResponse place;

    private Long distanceToNextMeters;
    private String formattedDistanceToNext;
    private Integer travelTimeToNextMinutes;
    private String formattedTravelTimeToNext;

    private String idealTimingTip;
}
