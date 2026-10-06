package com.tripmind.domains.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tripmind.enums.ExpenseCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

/**
 * Tổng hợp ngân sách một chuyến. Chi phí ước tính (từ hoạt động) và chi tiêu thực tế (từ
 * bảng {@code expenses}) là hai đại lượng tách rời (BR-402); {@code remaining} trừ đi phần
 * lớn hơn trong hai.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BudgetSummaryResponse {

    private Long tripId;
    private Long budget;
    private String currency;
    private Long estimatedTotal;
    private Long actualTotal;
    private Long remaining;
    /** max(ước tính, thực chi) ÷ ngân sách; null khi chưa đặt ngân sách. */
    private Double usedRatio;
    /** NONE (≤ 90%) · NEAR_LIMIT (> 90%) · OVER (> 100%). */
    private String warningLevel;
    private Map<ExpenseCategory, CategoryAmount> byCategory;

    /** "Hôm nay còn tiêu được bao nhiêu"; null kèm {@code dailyReason} khi không tính được. */
    private DailyAllowance daily;
    /** NO_BUDGET · TRIP_ENDED. */
    private String dailyReason;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryAmount {
        private Long estimated;
        private Long actual;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DailyAllowance {
        /** PLAN = chưa đi, bình quân mỗi ngày; TODAY = đang đi, hạn mức của hôm nay. */
        private String mode;
        /** Ngày tính hạn mức, theo múi giờ điểm đến. */
        private LocalDate date;
        /** true khi giai đoạn đang được đặt tay và "hôm nay" là ngày mô phỏng. */
        private Boolean simulated;
        private Integer dayNumber;
        /** Số ngày còn lại, tính cả hôm nay. */
        private Integer remainingDays;
        /** Hạn mức mỗi ngày; đứng yên suốt cả ngày, không co lại theo từng khoản chi của hôm nay. */
        private Long allowance;
        private Long spentToday;
        /** allowance − spentToday; có thể âm. */
        private Long todayLeft;
        /** ON_TRACK · NEAR · OVER. */
        private String status;
        /** Bình quân đã tiêu mỗi ngày đã đi qua của chuyến (không tính khoản chi trước ngày đi). */
        private Long averageSpentPerDay;
        /** Đã tiêu lố ngân sách bao nhiêu, khi ngân sách còn lại ≤ 0. */
        private Long overBy;
    }
}
