package com.tripmind.entities;

import com.fasterxml.jackson.databind.JsonNode;
import com.tripmind.enums.ProposalKind;
import com.tripmind.enums.ProposalStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Một đề xuất của trợ lý. Trợ lý chỉ ghi được vào bảng này (QĐ-01); thay đổi chỉ chạm tới
 * {@code activities} khi người dùng bấm áp dụng.
 */
@Entity
@Table(name = "ai_proposals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiProposalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "message_id")
    private Long messageId;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changes_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode changes;

    @Column(name = "estimated_cost_delta")
    private Long estimatedCostDelta;

    /** Chênh lệch thời gian di chuyển (phút) trên các ngày bị ảnh hưởng. */
    @Column(name = "travel_time_delta")
    private Integer travelTimeDelta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private ProposalStatus status = ProposalStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    @Builder.Default
    private ProposalKind kind = ProposalKind.ITINERARY;

    /** Lý do, công cụ đã chạy, ứng viên bị loại — nguồn của "Vì sao AI chọn chỗ này?". */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_json", columnDefinition = "jsonb")
    private JsonNode evidence;

    @Column(name = "applied_seq")
    private Long appliedSeq;

    /** Nhật ký thao tác nghịch đảo ghi lúc áp dụng; hoàn tác chạy theo nó. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "undo_json", columnDefinition = "jsonb")
    private JsonNode undo;

    @Column(name = "reverted_at")
    private Instant revertedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
