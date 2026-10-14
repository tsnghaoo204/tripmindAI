package com.tripmind.services.ai;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Instant;

/**
 * Cổng gọi mô hình ngôn ngữ. Dịch vụ nghiệp vụ không biết nhà cung cấp là ai; test thay bằng
 * một bản giả trả lời theo kịch bản.
 */
public interface LlmGateway {

    boolean isConfigured();

    /** Một lượt gọi mô hình, không tự chạy công cụ: tool call trả về trong phản hồi. */
    ChatResponse call(Prompt prompt);

    /**
     * Như {@link #call(Prompt)} nhưng không vượt quá {@code deadline} (kể cả thử lại và đổi mô
     * hình); quá hạn thì ném {@code AI_TIMEOUT}.
     */
    default ChatResponse call(Prompt prompt, Instant deadline) {
        return call(prompt);
    }

    /** Tin nhắn trả lời đầu tiên; {@code null} khi mô hình trả rỗng. */
    static AssistantMessage output(ChatResponse response) {
        if (response == null || response.getResult() == null) {
            return null;
        }
        return response.getResult().getOutput();
    }

    /** Văn bản trả lời; {@code null} khi mô hình trả rỗng. */
    static String text(ChatResponse response) {
        AssistantMessage output = output(response);
        return output == null ? null : output.getText();
    }
}
