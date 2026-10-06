package com.tripmind.domains.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tripmind.domains.models.GroupProfile;
import com.tripmind.enums.BudgetPreference;
import com.tripmind.enums.TravelStyle;
import com.tripmind.enums.TripPhase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Một chuyến đi. Danh sách chuyến không kèm {@code days}; xem chi tiết thì có tóm tắt từng
 * ngày, còn hoạt động đầy đủ nằm ở {@code GET /api/trips/{id}/itinerary}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TripResponse {

    private Long id;
    private String name;
    private DestinationResponse destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDays;
    private Short travelers;
    private Long budget;
    private String currency;
    private TravelStyle travelStyle;
    private BudgetPreference budgetPreference;
    private List<String> preferences;
    private GroupProfile groupProfile;

    /** Giai đoạn hiệu lực: BEFORE · DURING · AFTER. */
    private TripPhase phase;
    /** AUTO = suy từ ngày; MANUAL = người dùng tự đặt, giao diện phải ghi rõ. */
    private String phaseSource;
    /** Ngày hôm nay theo múi giờ điểm đến. */
    private LocalDate today;
    /** Phần trăm số ngày đã lên lịch (BR-207). */
    private Integer planningProgress;

    private List<DaySummary> days;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DaySummary {
        private Long id;
        private Short dayNumber;
        private LocalDate date;
        private String note;
        private Integer activityCount;
    }
}
