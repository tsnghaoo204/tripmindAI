package com.tripmind.domains.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.tripmind.enums.ToolExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolExecutionResponse {

    private Long id;
    private Long conversationId;
    private Long messageId;
    private Long tripId;
    private Long userId;
    private String toolName;
    private JsonNode arguments;
    private JsonNode result;
    private ToolExecutionStatus status;
    private String errorMessage;
    private Integer executionTimeMs;
    private Instant createdAt;

    public static ToolExecutionResponse fromEntity(com.tripmind.entities.AiToolExecutionEntity e, boolean withPayload) {
        return ToolExecutionResponse.builder()
                .id(e.getId())
                .conversationId(e.getConversationId())
                .messageId(e.getMessageId())
                .tripId(e.getTripId())
                .userId(e.getUserId())
                .toolName(e.getToolName())
                .arguments(withPayload ? e.getArguments() : null)
                .result(withPayload ? e.getResult() : null)
                .status(e.getStatus())
                .errorMessage(e.getErrorMessage())
                .executionTimeMs(e.getExecutionTimeMs())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
