package com.tripmind.services.ai;

import com.tripmind.configurations.properties.AiProperties;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

/**
 * Gemini qua lớp tương thích OpenAI (Spring AI). Dựng {@link OpenAiChatModel} bằng tay thay
 * vì dùng starter: không có khoá thì ứng dụng vẫn khởi động, chỉ trợ lý báo chưa cấu hình.
 */
@Slf4j
@Component
public class OpenAiLlmGateway implements LlmGateway {

    private final OpenAiChatModel chatModel;

    public OpenAiLlmGateway(AiProperties properties) {
        if (!properties.isConfigured()) {
            log.warn("GEMINI_API_KEY chua dat: tro ly AI se tra 503 AI_NOT_CONFIGURED");
            this.chatModel = null;
            return;
        }
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(properties.baseUrl())
                .completionsPath(properties.completionsPath())
                .apiKey(properties.apiKey())
                .build();
        this.chatModel = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(properties.model())
                        .temperature(properties.temperature())
                        .build())
                // Mặc định Spring AI thử lại 10 lần; một lượt hỏi chỉ có 60 giây.
                .retryTemplate(RetryTemplate.builder().maxAttempts(2).fixedBackoff(500).build())
                .build();
        log.info("Tro ly AI dung mo hinh {} tai {}", properties.model(), properties.baseUrl());
    }

    @Override
    public boolean isConfigured() {
        return chatModel != null;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        if (chatModel == null) {
            throw new AppException(ErrorCode.AI_NOT_CONFIGURED);
        }
        try {
            return chatModel.call(prompt);
        } catch (RuntimeException e) {
            log.error("Nha cung cap mo hinh loi: {}", e.getMessage());
            throw new AppException(ErrorCode.AI_PROVIDER_ERROR, "AI model provider failed: " + e.getMessage());
        }
    }
}
