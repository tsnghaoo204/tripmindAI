package com.tripmind.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
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
public abstract class IntegrationTest {

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

    protected JsonNode get(String url, String token, int expectedStatus) throws Exception {
        return call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url), token, null, expectedStatus);
    }
}
