package com.tripmind.support;

import com.tripmind.services.ai.LlmGateway;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * Mô hình giả trả lời theo kịch bản cho test tích hợp. Mỗi bước kịch bản nhận prompt và trả
 * một phản hồi; hết kịch bản thì dùng bước mặc định (trả lời "OK").
 */
public class FakeLlmGateway implements LlmGateway {

    private final Deque<Function<Prompt, ChatResponse>> script = new ArrayDeque<>();
    private final List<Prompt> prompts = new CopyOnWriteArrayList<>();
    private volatile Function<Prompt, ChatResponse> fallback = p -> text("OK");
    private volatile boolean configured = true;

    public synchronized void reset() {
        script.clear();
        prompts.clear();
        fallback = p -> text("OK");
        configured = true;
    }

    public synchronized FakeLlmGateway then(Function<Prompt, ChatResponse> step) {
        script.add(step);
        return this;
    }

    public FakeLlmGateway thenToolCalls(Map<String, String> calls) {
        return then(p -> toolCalls(calls));
    }

    public FakeLlmGateway thenText(String text) {
        return then(p -> text(text));
    }

    public void setFallback(Function<Prompt, ChatResponse> fallback) {
        this.fallback = fallback;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }

    public List<Prompt> prompts() {
        return new ArrayList<>(prompts);
    }

    @Override
    public boolean isConfigured() {
        return configured;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        prompts.add(prompt);
        Function<Prompt, ChatResponse> step;
        synchronized (this) {
            step = script.isEmpty() ? fallback : script.poll();
        }
        return step.apply(prompt);
    }

    public static ChatResponse text(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    /** Tên công cụ → tham số JSON. */
    public static ChatResponse toolCalls(Map<String, String> calls) {
        List<AssistantMessage.ToolCall> toolCalls = calls.entrySet().stream()
                .map(e -> new AssistantMessage.ToolCall(UUID.randomUUID().toString(), "function", e.getKey(), e.getValue()))
                .toList();
        return new ChatResponse(List.of(new Generation(new AssistantMessage("", Map.of(), toolCalls))));
    }
}
