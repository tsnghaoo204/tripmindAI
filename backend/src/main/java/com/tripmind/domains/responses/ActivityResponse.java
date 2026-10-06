package com.tripmind.domains.responses;

import com.tripmind.enums.ActivityCreator;
import com.tripmind.enums.ActivityStatus;
import com.tripmind.enums.ActivityType;
import com.tripmind.enums.CostSource;
import com.tripmind.enums.SkipReason;
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
    private ActivityCreator createdBy;
    /** Đề xuất AI sinh ra hoạt động này; NULL nếu người dùng tự thêm. */
    private Long fromProposalId;
    private LocalTime startTime;
    private LocalTime endTime;
    private Long estimatedCost;
    private CostSource estimatedCostSource;
    private String transportationMode;
    private String notes;
    private ActivityStatus status;
    private SkipReason skipReason;
    private LocalTime actualStart;
    private LocalTime actualEnd;

    private PlaceResponse place;

    private Long distanceToNextMeters;
    private String formattedDistanceToNext;
    private Integer travelTimeToNextMinutes;
    private String formattedTravelTimeToNext;

    private String idealTimingTip;
}
