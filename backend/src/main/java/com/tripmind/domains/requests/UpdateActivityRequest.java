package com.tripmind.domains.requests;

import com.tripmind.enums.ActivityStatus;
import com.tripmind.enums.ActivityType;
import com.tripmind.enums.CostSource;
import com.tripmind.enums.SkipReason;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/** Sửa một hoạt động. Trường nào null thì giữ nguyên. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateActivityRequest {

    @Size(max = 200, message = "Activity title cannot exceed 200 characters")
    private String title;
    private ActivityType activityType;
    private Long placeId;
    private String placeProvider;
    private String placeExternalId;

    private LocalTime startTime;

    private LocalTime endTime;

    @Min(value = 0, message = "Estimated cost must not be negative")
    private Long estimatedCost;
    private CostSource estimatedCostSource;

    @Size(max = 16)
    private String transportationMode;
    private String notes;

    /** Đánh dấu thực hiện. {@code SKIPPED} bắt buộc kèm {@code skipReason} (FR-1004). */
    private ActivityStatus status;
    private SkipReason skipReason;

    private LocalTime actualStart;

    private LocalTime actualEnd;
}
