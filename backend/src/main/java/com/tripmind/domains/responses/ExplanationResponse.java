package com.tripmind.domains.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.tripmind.enums.ActivityCreator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * "Vì sao AI chọn chỗ này?" (FR-1208 → FR-1210). Mọi dòng ở đây là dữ liệu thật trong nhật
 * ký; không truy được nguồn thì {@code known = false}, không dựng lời giải thích.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExplanationResponse {

    private Long activityId;
    private ActivityCreator createdBy;
    private boolean known;
    /** Vì sao không có giải trình, khi {@code known = false}. */
    private String unknownReason;
    private Long proposalId;
    private String proposalSummary;
    private String proposalReason;
    /** Lý do riêng của thao tác đã tạo ra hoạt động này. */
    private String changeReason;
    private Instant appliedAt;
    private List<ToolExecutionResponse> tools;
    /** Ứng viên / thao tác bị loại và vì sao. */
    private JsonNode rejected;
}
