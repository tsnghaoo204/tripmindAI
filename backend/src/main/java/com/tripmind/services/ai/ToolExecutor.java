package com.tripmind.services.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripmind.configurations.properties.AiProperties;
import com.tripmind.entities.AiToolExecutionEntity;
import com.tripmind.enums.ToolExecutionStatus;
import com.tripmind.repositories.AiToolExecutionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Chạy một tool call do mô hình yêu cầu: chặn gọi lặp, đo thời gian, giới hạn theo hạn chót
 * của lượt hỏi, cắt bớt kết quả, và <b>luôn</b> ghi một dòng {@code ai_tool_executions} — kể
 * cả khi công cụ lỗi hoặc quá hạn (BR-505). Lỗi không ném ra ngoài: mô hình nhận một kết quả
 * {@code {"error": ...}} để tự xoay xở.
 */
@Slf4j
@Component
public class ToolExecutor {

    private final ToolRegistry registry;
    private final AiToolExecutionRepository executionRepository;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor;

    public ToolExecutor(ToolRegistry registry, AiToolExecutionRepository executionRepository, AiProperties properties,
                        ObjectMapper objectMapper, @Qualifier("aiExecutor") ExecutorService executor) {
        this.registry = registry;
        this.executionRepository = executionRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.executor = executor;
    }

    public String execute(ToolCall call, AgentTurn turn) {
        String arguments = call.arguments() == null || call.arguments().isBlank() ? "{}" : call.arguments();
        String dedupeKey = call.name() + ":" + normalize(arguments);
        String previous = turn.getResultsByCall().get(dedupeKey);
        if (previous != null) {
            log.debug("Chan goi lap {} trong cung luot hoi", call.name());
            return previous;
        }

        turn.emit("tool_start", Map.of("tool", call.name(), "label", registry.label(call.name())));
        long started = System.nanoTime();
        ToolExecutionStatus status = ToolExecutionStatus.OK;
        String error = null;
        String result;

        ToolCallback callback = registry.find(call.name());
        if (callback == null) {
            status = ToolExecutionStatus.ERROR;
            error = "Unknown tool " + call.name();
            result = errorJson(error);
        } else {
            long remainingMs = Duration.between(Instant.now(), turn.getDeadline()).toMillis();
            Future<String> future = executor.submit(() -> callback.call(arguments,
                    new ToolContext(Map.of(AgentTurn.CONTEXT_KEY, turn))));
            try {
                result = future.get(Math.max(remainingMs, 1), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                status = ToolExecutionStatus.TIMEOUT;
                error = "Tool timed out";
                result = errorJson(error);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                status = ToolExecutionStatus.ERROR;
                error = rootMessage(cause);
                result = errorJson(error);
                log.warn("Cong cu {} loi: {}", call.name(), error);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                status = ToolExecutionStatus.ERROR;
                error = "Interrupted";
                result = errorJson(error);
            }
        }
        int elapsedMs = (int) ((System.nanoTime() - started) / 1_000_000);
        String truncated = truncate(result);

        AiToolExecutionEntity saved = executionRepository.save(AiToolExecutionEntity.builder()
                .conversationId(turn.getConversationId())
                .tripId(turn.getTripId())
                .userId(turn.getUserId())
                .toolName(call.name())
                .arguments(toJson(arguments))
                .result(toJson(truncated))
                .status(status)
                .errorMessage(error)
                .executionTimeMs(elapsedMs)
                .build());
        turn.getExecutionIds().add(saved.getId());
        turn.getResultsByCall().put(dedupeKey, truncated);
        turn.emit("tool_end", Map.of("tool", call.name(), "ms", elapsedMs, "status", status.name()));
        return truncated;
    }

    private String normalize(String arguments) {
        try {
            return objectMapper.writeValueAsString(objectMapper.readTree(arguments));
        } catch (Exception e) {
            return arguments.trim();
        }
    }

    private JsonNode toJson(String text) {
        try {
            return objectMapper.readTree(text);
        } catch (Exception e) {
            return objectMapper.createObjectNode().put("text", text);
        }
    }

    /** Kết quả quá dài thì đưa cho mô hình phần đầu kèm cờ {@code truncated}. */
    private String truncate(String result) {
        int max = properties.toolResultMaxChars();
        if (result == null) {
            return "null";
        }
        if (result.length() <= max) {
            return result;
        }
        return objectMapper.createObjectNode()
                .put("truncated", true)
                .put("partial", result.substring(0, max))
                .toString();
    }

    private String errorJson(String message) {
        return objectMapper.createObjectNode().put("error", message).toString();
    }

    private String rootMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
    }
}
