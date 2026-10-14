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

import jakarta.annotation.PreDestroy;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Gọi LLM qua 9router, thử lần lượt các model trong {@code LLM_MODEL}. Chỉ thử lại lỗi tạm thời
 * (5xx, mất kết nối); tổng thời gian bị chặn bởi hạn chót của người gọi.
 */
@Slf4j
@Component
public class OpenAiLlmGateway implements LlmGateway {

    private static final Duration DEFAULT_BUDGET = Duration.ofMinutes(2);

    private final List<NamedModel> chatModels;
    private final ExecutorService callExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private record NamedModel(String name, OpenAiChatModel model) {
    }

    public OpenAiLlmGateway(AiProperties properties) {
        if (!properties.isConfigured()) {
            log.warn("LLM_API_KEY hoac LLM_MODEL chua dat: tro ly AI se tra 503 AI_NOT_CONFIGURED");
            this.chatModels = List.of();
            return;
        }
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
        this.chatModels = properties.models().stream()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .distinct()
                .map(name -> new NamedModel(name, OpenAiChatModel.builder()
                        .openAiApi(api)
                        .defaultOptions(OpenAiChatOptions.builder()
                                .model(name)
                                .temperature(properties.temperature())
                                .build())
                        .retryTemplate(RetryTemplate.builder()
                                .maxAttempts(2)
                                .fixedBackoff(1000)
                                .retryOn(List.of(TransientAiException.class, ResourceAccessException.class))
                                .traversingCauses()
                                .build())
                        .build()))
                .toList();

        log.info("Tro ly AI dung cac mo hinh {} tai {}",
                chatModels.stream().map(NamedModel::name).toList(), properties.baseUrl());
    }

    @Override
    public boolean isConfigured() {
        return !chatModels.isEmpty();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        return call(prompt, Instant.now().plus(DEFAULT_BUDGET));
    }

    /** Thử lần lượt các mô hình, tổng thời gian không vượt {@code deadline}. */
    @Override
    public ChatResponse call(Prompt prompt, Instant deadline) {
        if (chatModels.isEmpty()) {
            throw new AppException(ErrorCode.AI_NOT_CONFIGURED);
        }
        RuntimeException lastError = null;
        for (NamedModel candidate : chatModels) {
            long remainingMs = Duration.between(Instant.now(), deadline).toMillis();
            if (remainingMs <= 0) {
                break;
            }
            Future<ChatResponse> future = callExecutor.submit(() -> candidate.model().call(prompt));
            try {
                return future.get(remainingMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                throw new AppException(ErrorCode.AI_TIMEOUT, "AI model " + candidate.name() + " did not answer in time");
            } catch (ExecutionException e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                lastError = cause instanceof RuntimeException re ? re : new RuntimeException(cause);
                log.warn("Mo hinh [{}] loi ({}), chuyen sang mo hinh tiep theo", candidate.name(), cause.getMessage());
            } catch (InterruptedException e) {
                future.cancel(true);
                Thread.currentThread().interrupt();
                throw new AppException(ErrorCode.AI_PROVIDER_ERROR, "Interrupted while waiting for the AI model");
            }
        }
        if (lastError == null) {
            throw new AppException(ErrorCode.AI_TIMEOUT, "No time left to call the AI model");
        }
        throw new AppException(ErrorCode.AI_PROVIDER_ERROR, "All AI models failed: " + lastError.getMessage());
    }

    @PreDestroy
    void shutdown() {
        callExecutor.shutdownNow();
    }
}
