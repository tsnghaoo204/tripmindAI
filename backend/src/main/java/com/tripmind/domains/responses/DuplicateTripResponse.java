package com.tripmind.domains.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Chuyến mới sau khi nhân bản. {@code previouslySkipped} là các hoạt động đã bị bỏ ở chuyến cũ
 * nhưng vẫn được sao chép — giao diện hỏi người dùng có muốn giữ không.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateTripResponse {

    private TripResponse trip;
    private int activitiesCopied;
    private int checklistItemsCopied;
    private List<SkippedActivity> previouslySkipped;

    public record SkippedActivity(Long activityId, Integer dayNumber, String title, String skipReason) {
    }
}
