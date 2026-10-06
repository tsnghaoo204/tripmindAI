package com.tripmind.configurations.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Trợ lý AI, cấu hình ở {@code tripmind.ai.*}. Mô hình gọi qua lớp tương thích OpenAI nên
 * đổi nhà cung cấp chỉ cần đổi {@code baseUrl}, {@code apiKey}, {@code model}.
 *
 * @param apiKey             để trống thì trợ lý trả {@code 503 AI_NOT_CONFIGURED}, phần còn lại vẫn chạy
 * @param maxRounds          trần số vòng gọi mô hình trong một lượt hỏi (BR-502)
 * @param turnTimeout        trần thời gian một lượt hỏi (BR-502)
 * @param historyMessages    số tin nhắn gần nhất đưa lại vào ngữ cảnh
 * @param toolResultMaxChars cắt bớt kết quả công cụ dài hơn mức này trước khi đưa cho mô hình
 * @param rateLimitPerHour   số lượt hỏi tối đa mỗi giờ mỗi người (BR-601)
 * @param proposalTtl        đề xuất hết hạn sau khoảng này (FR-707)
 * @param undoWindow         cửa sổ hoàn tác sau khi áp dụng (FR-1201)
 */
@Validated
@ConfigurationProperties(prefix = "tripmind.ai")
public record AiProperties(
        String apiKey,
        @NotBlank String baseUrl,
        @NotBlank String completionsPath,
        @NotBlank String model,
        @DefaultValue("0.3") double temperature,
        @DefaultValue("8") @Min(1) @Max(20) int maxRounds,
        @DefaultValue("PT60S") Duration turnTimeout,
        @DefaultValue("20") @Min(0) int historyMessages,
        @DefaultValue("4000") @Min(500) int toolResultMaxChars,
        @DefaultValue("5") @Min(1) @Max(10) int maxSearchResults,
        @DefaultValue("20") @Min(1) int rateLimitPerHour,
        @DefaultValue("PT30M") Duration proposalTtl,
        @DefaultValue("PT10M") Duration undoWindow) {

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
