package com.tripmind.entities;

import com.fasterxml.jackson.databind.JsonNode;
import com.tripmind.enums.MessageRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private MessageRole role;

    @Column(columnDefinition = "text")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tool_calls_json", columnDefinition = "jsonb")
    private JsonNode toolCalls;

    @Column(name = "tool_call_id", length = 64)
    private String toolCallId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "token_usage", columnDefinition = "jsonb")
    private JsonNode tokenUsage;

    /** Đề xuất, ứng viên địa điểm, gợi ý checklist đi kèm tin nhắn trợ lý. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attachments_json", columnDefinition = "jsonb")
    private JsonNode attachments;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
