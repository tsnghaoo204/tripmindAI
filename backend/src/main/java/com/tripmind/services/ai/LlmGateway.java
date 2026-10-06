package com.tripmind.services.ai;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Cổng gọi mô hình ngôn ngữ. Dịch vụ nghiệp vụ không biết nhà cung cấp là ai; test thay bằng
 * một bản giả trả lời theo kịch bản.
 */
public interface LlmGateway {

    boolean isConfigured();

    /** Một lượt gọi mô hình, không tự chạy công cụ: tool call trả về trong phản hồi. */
    ChatResponse call(Prompt prompt);
}
