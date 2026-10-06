package com.tripmind.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Nền cho test tích hợp: PostgreSQL 16 và Redis thật trong Docker, Flyway chạy toàn bộ
 * migration lên CSDL trống. Container dùng chung cho mọi lớp test con (khởi động một lần).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTest.FakeLlmConfiguration.class)
public abstract class IntegrationTest {

    @TestConfiguration
    static class FakeLlmConfiguration {
        @Bean
        @Primary
        FakeLlmGateway fakeLlmGateway() {
            return new FakeLlmGateway();
        }
    }

    @Autowired
    protected FakeLlmGateway llm;

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));

    @ServiceConnection(name = "redis")
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /** Đăng ký một người dùng mới, trả về phiếu truy cập. */
    protected String registerUser() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@tripmind.test";
        JsonNode body = call(post("/api/auth/register"), null,
                Map.of("email", email, "password", "secret123", "name", "Tester"), 201);
        return body.at("/data/accessToken").asText();
    }

    /** Tạo chuyến ở điểm đến gieo sẵn đầu tiên, trả về {@code data}. */
    protected JsonNode createTrip(String token, String startDate, String endDate) throws Exception {
        return call(post("/api/trips"), token, Map.of(
                "destinationId", 1,
                "name", "Chuyen test",
                "startDate", startDate,
                "endDate", endDate,
                "travelers", 2,
                "budget", 8_000_000), 201).get("data");
    }

    protected JsonNode call(MockHttpServletRequestBuilder request, String token, Object body, int expectedStatus)
            throws Exception {
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        }
        MvcResult result = mockMvc.perform(request).andReturn();
        String content = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        if (result.getResponse().getStatus() != expectedStatus) {
            throw new AssertionError("Expected HTTP " + expectedStatus + " but got "
                    + result.getResponse().getStatus() + ": " + content);
        }
        return content.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(content);
    }

    /**
     * Gửi một câu hỏi tới trợ lý, đợi luồng SSE đóng, trả về toàn bộ văn bản sự kiện.
     */
    protected String chat(String token, long tripId, Object body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/trips/" + tripId + "/ai/chat")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
        if (!result.getRequest().isAsyncStarted()) {
            throw new AssertionError("Chat did not start a stream: HTTP " + result.getResponse().getStatus()
                    + " " + result.getResponse().getContentAsString());
        }
        long deadline = System.currentTimeMillis() + 20_000;
        String content = "";
        while (System.currentTimeMillis() < deadline) {
            content = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            if (content.contains("event:done") || content.contains("event:error")) {
                break;
            }
            Thread.sleep(20);
        }
        return content;
    }

    /** Dữ liệu JSON của sự kiện SSE đầu tiên có tên {@code event}. */
    protected JsonNode eventData(String stream, String event) throws Exception {
        for (String block : stream.split("\\n\\n")) {
            if (block.contains("event:" + event + "\n")) {
                for (String line : block.split("\\n")) {
                    if (line.startsWith("data:")) {
                        return objectMapper.readTree(line.substring(5));
                    }
                }
            }
        }
        return null;
    }

    protected JsonNode get(String url, String token, int expectedStatus) throws Exception {
        return call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url), token, null, expectedStatus);
    }
}
