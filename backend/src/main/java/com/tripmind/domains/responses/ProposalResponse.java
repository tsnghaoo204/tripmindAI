package com.tripmind.domains.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.tripmind.enums.ProposalKind;
import com.tripmind.enums.ProposalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Đủ dữ liệu để vẽ thẻ so sánh trước/sau (FR-704): mỗi thao tác trong {@code changes} có
 * {@code op}, ảnh chụp {@code before} (với REMOVE/UPDATE) và giá trị mới.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProposalResponse {

    private Long id;
    private Long tripId;
    private Long conversationId;
    private ProposalKind kind;
    private ProposalStatus status;
    private String summary;
    private String reason;
    private JsonNode changes;
    /** Thao tác mô hình gửi nhưng bị loại lúc dựng, kèm lý do. */
    private JsonNode rejected;
    private Long estimatedCostDelta;
    /** Phút di chuyển tăng (+) hoặc giảm (−) trên các ngày bị ảnh hưởng. */
    private Integer travelTimeDelta;
    private Instant expiresAt;
    private Instant appliedAt;
    private Instant revertedAt;
    private Instant undoAvailableUntil;
}
