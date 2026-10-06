package com.tripmind.controllers;

import com.tripmind.configurations.properties.AiProperties;
import com.tripmind.configurations.security.SecurityUtils;
import com.tripmind.domains.requests.AiChatRequest;
import com.tripmind.domains.responses.ApiResponse;
import com.tripmind.domains.responses.ConversationResponse;
import com.tripmind.domains.responses.MessageResponse;
import com.tripmind.domains.responses.ToolExecutionResponse;
import com.tripmind.exceptions.AppException;
import com.tripmind.services.ai.AgentService;
import com.tripmind.services.ai.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

@Slf4j
@RestController
@Tag(name = "AI Assistant", description = "Trip-aware assistant with tool calling, streamed as Server-Sent Events")
@SecurityRequirement(name = "bearerAuth")
public class AiController {

    private final AgentService agentService;
    private final ConversationService conversationService;
    private final AiProperties aiProperties;
    private final ExecutorService executor;

    public AiController(AgentService agentService, ConversationService conversationService, AiProperties aiProperties,
                        @Qualifier("aiExecutor") ExecutorService executor) {
        this.agentService = agentService;
        this.conversationService = conversationService;
        this.aiProperties = aiProperties;
        this.executor = executor;
    }

    /**
     * Mọi kiểm tra (sở hữu chuyến, cấu hình AI, giới hạn tần suất) chạy <b>trước</b> khi mở
     * luồng, nên lỗi trả về đúng mã HTTP dạng JSON. Sau đó luồng gửi các sự kiện
     * {@code tool_start · tool_end · token · proposal · places · done · error}.
     */
    @PostMapping("/api/trips/{tripId}/ai/chat")
    @Operation(summary = "Ask the assistant about this trip (text/event-stream)")
    public SseEmitter chat(@PathVariable Long tripId, @Valid @RequestBody AiChatRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        agentService.precheck(userId, tripId, request.getConversationId());

        SseEmitter emitter = new SseEmitter(aiProperties.turnTimeout().plusSeconds(30).toMillis());
        executor.submit(() -> {
            try {
                agentService.runTurn(userId, tripId, request, (event, data) -> send(emitter, event, data));
                emitter.complete();
            } catch (AppException e) {
                send(emitter, "error", Map.of("code", e.getErrorCode().name(), "message", e.getMessage()));
                emitter.complete();
            } catch (Exception e) {
                log.error("Luot hoi tro ly loi", e);
                send(emitter, "error", Map.of("code", "INTERNAL_SERVER_ERROR", "message", "Assistant failed"));
                emitter.complete();
            }
        });
        return emitter;
    }

    @GetMapping("/api/trips/{tripId}/ai/conversations")
    @Operation(summary = "Conversations of a trip, newest first")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> conversations(@PathVariable Long tripId) {
        return ResponseEntity.ok(ApiResponse.ok(conversationService.list(SecurityUtils.getCurrentUserId(), tripId)));
    }

    @GetMapping("/api/conversations/{conversationId}/messages")
    @Operation(summary = "Messages of a conversation, oldest first, with attachments")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> messages(@PathVariable Long conversationId) {
        return ResponseEntity.ok(ApiResponse.ok(conversationService.messages(SecurityUtils.getCurrentUserId(), conversationId)));
    }

    @GetMapping("/api/trips/{tripId}/ai/activity")
    @Operation(summary = "Tool executions of this trip (the assistant's audit log)")
    public ResponseEntity<ApiResponse<List<ToolExecutionResponse>>> activity(
            @PathVariable Long tripId, @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(conversationService.activity(SecurityUtils.getCurrentUserId(), tripId,
                Math.min(Math.max(limit, 1), 200))));
    }

    private void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            log.debug("Client da dong luong SSE ({}): {}", event, e.getMessage());
        }
    }
}
