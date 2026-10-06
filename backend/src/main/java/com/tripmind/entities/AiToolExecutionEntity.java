package com.tripmind.entities;

import com.fasterxml.jackson.databind.JsonNode;
import com.tripmind.enums.ToolExecutionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/** Một lần trợ lý chạy công cụ. Ghi cả khi lỗi hoặc quá hạn (BR-505). */
@Entity
@Table(name = "ai_tool_executions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiToolExecutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id")
    private Long conversationId;

    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "trip_id")
    private Long tripId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "tool_name", nullable = false, length = 64)
    private String toolName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode arguments;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode result;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private ToolExecutionStatus status;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "execution_time_ms", nullable = false)
    private int executionTimeMs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
