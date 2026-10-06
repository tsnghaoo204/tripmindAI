package com.tripmind.services.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tripmind.configurations.properties.AiProperties;
import com.tripmind.domains.requests.AiChatRequest;
import com.tripmind.entities.ConversationEntity;
import com.tripmind.entities.MessageEntity;
import com.tripmind.entities.TripEntity;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import com.tripmind.services.RateLimiter;
import com.tripmind.services.TripService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Vòng lặp agent tự điều khiển (không để Spring AI tự chạy công cụ) để chèn được: trần 8 vòng
 * / 60 giây (BR-502), nhật ký từng lần chạy công cụ (BR-505), chặn gọi lặp (BR-503) và sự
 * kiện tiến trình gửi về giao diện. Chạm trần thì trả lời bằng dữ liệu đang có, không báo lỗi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private static final Duration MIN_TIME_FOR_FINAL_ANSWER = Duration.ofSeconds(3);
    private static final int TOKEN_CHUNK_CHARS = 24;
    private static final String WRAP_UP_INSTRUCTION =
            "Hệ thống: đã chạm giới hạn số bước của lượt này. Hãy trả lời ngay bằng dữ liệu đã có, không gọi thêm công cụ.";
    private static final String FALLBACK_ANSWER =
            "Mình chưa kịp hoàn tất trong thời gian cho phép. Bạn thử hỏi lại ngắn gọn hơn nhé.";

    private final LlmGateway llm;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final ConversationService conversationService;
    private final SystemPromptBuilder promptBuilder;
    private final TripService tripService;
    private final RateLimiter rateLimiter;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * Kiểm tra trước khi mở luồng (ADS-30 §9.1): chuyến thuộc người hỏi, hội thoại (nếu có)
     * thuộc chuyến, trợ lý đã cấu hình, chưa vượt giới hạn tần suất tầng AI.
     */
    public void precheck(Long userId, Long tripId, Long conversationId) {
        tripService.getOwnedTrip(userId, tripId);
        if (conversationId != null) {
            conversationService.requireConversation(userId, tripId, conversationId);
        }
        if (!llm.isConfigured()) {
            throw new AppException(ErrorCode.AI_NOT_CONFIGURED, "AI assistant is not configured (GEMINI_API_KEY is empty)");
        }
        rateLimiter.check("ai", userId, properties.rateLimitPerHour(), Duration.ofHours(1));
    }

    public void runTurn(Long userId, Long tripId, AiChatRequest request, AgentEventSink sink) {
        TripEntity trip = tripService.getOwnedTrip(userId, tripId);
        ConversationEntity conversation = conversationService.openOrCreate(userId, tripId, request.getConversationId(),
                request.getMessage());
        MessageEntity userMessage = conversationService.saveUserMessage(conversation.getId(), request.getMessage());

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(promptBuilder.build(trip)));
        messages.addAll(conversationService.history(conversation.getId(), userMessage.getId()));
        messages.add(new UserMessage(request.getMessage()));

        AgentTurn turn = new AgentTurn(userId, tripId, conversation.getId(),
                Instant.now().plus(properties.turnTimeout()), sink);
        OpenAiChatOptions withTools = OpenAiChatOptions.builder()
                .toolCallbacks(toolRegistry.all())
                .internalToolExecutionEnabled(false)
                .build();

        int promptTokens = 0;
        int completionTokens = 0;
        int rounds = 0;
        String answer = null;
        while (rounds < properties.maxRounds() && Instant.now().isBefore(turn.getDeadline())) {
            rounds++;
            ChatResponse response = llm.call(new Prompt(messages, withTools));
            Usage usage = response.getMetadata() == null ? null : response.getMetadata().getUsage();
            if (usage != null) {
                promptTokens += nz(usage.getPromptTokens());
                completionTokens += nz(usage.getCompletionTokens());
            }
            AssistantMessage output = response.getResult().getOutput();
            if (!output.hasToolCalls()) {
                answer = output.getText();
                break;
            }
            messages.add(output);
            List<ToolResponseMessage.ToolResponse> results = new ArrayList<>();
            for (AssistantMessage.ToolCall call : output.getToolCalls()) {
                results.add(new ToolResponseMessage.ToolResponse(call.id(), call.name(), toolExecutor.execute(call, turn)));
            }
            messages.add(new ToolResponseMessage(results));
        }

        if (answer == null) {
            answer = wrapUp(messages, turn);
            log.info("Luot hoi trip ID={} cham tran sau {} vong", tripId, rounds);
        }

        streamText(answer, turn);
        ObjectNode usageJson = objectMapper.createObjectNode()
                .put("promptTokens", promptTokens)
                .put("completionTokens", completionTokens)
                .put("totalTokens", promptTokens + completionTokens)
                .put("rounds", rounds)
                .put("toolCalls", turn.getExecutionIds().size());
        MessageEntity saved = conversationService.saveAssistantMessage(conversation.getId(), answer, usageJson,
                attachments(turn), turn.getExecutionIds());
        sink.send("done", Map.of("messageId", saved.getId(), "conversationId", conversation.getId()));
    }

    /** Hết vòng hoặc hết giờ: hỏi mô hình một lần cuối, không cho gọi công cụ. */
    private String wrapUp(List<Message> messages, AgentTurn turn) {
        if (Duration.between(Instant.now(), turn.getDeadline()).compareTo(MIN_TIME_FOR_FINAL_ANSWER) < 0) {
            return FALLBACK_ANSWER;
        }
        messages.add(new UserMessage(WRAP_UP_INSTRUCTION));
        AssistantMessage output = llm.call(new Prompt(messages, OpenAiChatOptions.builder().build()))
                .getResult().getOutput();
        return output.getText() == null || output.getText().isBlank() ? FALLBACK_ANSWER : output.getText();
    }

    /** Câu trả lời cuối gửi về thành nhiều sự kiện {@code token} để giao diện hiện dần. */
    private void streamText(String text, AgentTurn turn) {
        int i = 0;
        while (i < text.length()) {
            int end = Math.min(text.length(), i + TOKEN_CHUNK_CHARS);
            int space = text.indexOf(' ', end);
            if (space > 0 && space - end < TOKEN_CHUNK_CHARS) {
                end = space + 1;
            }
            turn.emit("token", Map.of("text", text.substring(i, end)));
            i = end;
        }
    }

    private ObjectNode attachments(AgentTurn turn) {
        if (turn.getProposalIds().isEmpty() && turn.getAttachments().isEmpty()) {
            return null;
        }
        ObjectNode node = objectMapper.createObjectNode();
        if (!turn.getProposalIds().isEmpty()) {
            node.set("proposalIds", objectMapper.valueToTree(turn.getProposalIds()));
        }
        turn.getAttachments().forEach(node::set);
        return node;
    }

    private static int nz(Integer value) {
        return value == null ? 0 : value;
    }
}
