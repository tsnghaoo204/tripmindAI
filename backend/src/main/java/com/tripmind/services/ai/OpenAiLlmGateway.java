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
    private final OpenAiChatModel fallbackChatModel;

    public OpenAiLlmGateway(AiProperties properties) {
        if (!properties.isConfigured()) {
            log.warn("GEMINI_API_KEY chua dat: tro ly AI se tra 503 AI_NOT_CONFIGURED");
            this.chatModel = null;
            this.fallbackChatModel = null;
            return;
        }
        io.netty.channel.ChannelOption.class.getName(); // verify class availability
        reactor.netty.http.client.HttpClient httpClient = reactor.netty.http.client.HttpClient.create()
                .responseTimeout(java.time.Duration.ofSeconds(60))
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 15000)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new io.netty.handler.timeout.ReadTimeoutHandler(60, java.util.concurrent.TimeUnit.SECONDS))
                        .addHandlerLast(new io.netty.handler.timeout.WriteTimeoutHandler(60, java.util.concurrent.TimeUnit.SECONDS)));

        org.springframework.web.reactive.function.client.WebClient.Builder webClientBuilder =
                org.springframework.web.reactive.function.client.WebClient.builder()
                        .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(httpClient));

        org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(15000);
        requestFactory.setReadTimeout(60000);
        org.springframework.web.client.RestClient.Builder restClientBuilder =
                org.springframework.web.client.RestClient.builder().requestFactory(requestFactory);

        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(properties.baseUrl())
                .completionsPath(properties.completionsPath())
                .apiKey(properties.apiKey())
                .restClientBuilder(restClientBuilder)
                .webClientBuilder(webClientBuilder)
                .build();
        this.chatModel = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(properties.model())
                        .temperature(properties.temperature())
                        .build())
                .retryTemplate(RetryTemplate.builder()
                        .maxAttempts(3)
                        .exponentialBackoff(1000, 2.0, 4000)
                        .build())
                .build();

        String fallbackModel = "gemini-3.5-flash-lite".equalsIgnoreCase(properties.model())
                ? "gemini-3.1-flash-lite"
                : "gemini-3.5-flash-lite";

        this.fallbackChatModel = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(fallbackModel)
                        .temperature(properties.temperature())
                        .build())
                .retryTemplate(RetryTemplate.builder()
                        .maxAttempts(2)
                        .fixedBackoff(1000)
                        .build())
                .build();

        log.info("Tro ly AI dung mo hinh chinh [{}] va du phong [{}] tai {}", properties.model(), fallbackModel, properties.baseUrl());
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
            log.warn("Mo hinh chinh loi ({}). Tu dong chuyen sang mo hinh du phong...", e.getMessage());
            if (fallbackChatModel != null) {
                try {
                    return fallbackChatModel.call(prompt);
                } catch (RuntimeException fe) {
                    log.error("Mo hinh du phong cung loi: {}", fe.getMessage());
                }
            }
            throw new AppException(ErrorCode.AI_PROVIDER_ERROR, "AI model provider failed: " + e.getMessage());
        }
    }
}
