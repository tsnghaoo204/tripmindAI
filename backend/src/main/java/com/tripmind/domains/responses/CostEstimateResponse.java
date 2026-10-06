package com.tripmind.domains.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tripmind.enums.CostSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Gợi ý chi phí ước tính cho một hoạt động. Không đủ dữ liệu thì {@code suggested = null}
 * kèm {@code reason}: hệ thống không đoán.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CostEstimateResponse {

    /** Tổng cho cả nhóm, đã làm tròn xuống. */
    private Long suggested;
    /** Khoảng giá cho cả nhóm. */
    private Long min;
    private Long max;
    private String currency;
    private Integer travelers;
    private Integer priceLevel;
    /** Câu giải thích hiện nguyên văn, ví dụ "Mức giá $$ trên Google · 100k–300k/người × 2 người". */
    private String basis;
    private CostSource source;
    /** NO_PRICE_DATA · UNSUPPORTED_CURRENCY · UNSUPPORTED_ACTIVITY_TYPE. */
    private String reason;
}
