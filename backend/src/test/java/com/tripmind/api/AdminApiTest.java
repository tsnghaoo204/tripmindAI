package com.tripmind.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tripmind.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AdminApiTest extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Quản trị đọc được nhật ký công cụ, thống kê AI, danh sách tổng hợp; người dùng thường nhận 403")
    void adminEndpoints() throws Exception {
        llm.reset();
        String user = registerUser();
        long tripId = createTrip(user, LocalDate.now().plusDays(3).toString(), LocalDate.now().plusDays(3).toString())
                .get("id").asLong();
        llm.thenToolCalls(Map.of("get_current_trip", "{}")).thenText("Xong");
        chat(user, tripId, Map.of("message", "Chuyến của tôi?"));

        get("/api/admin/tool-executions", user, 403);

        String admin = registerUser();
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE id = (SELECT MAX(id) FROM users)");

        JsonNode executions = get("/api/admin/tool-executions?tool=get_current_trip&status=OK&size=5", admin, 200).get("data");
        assertThat(executions.get("totalElements").asLong()).isPositive();
        assertThat(executions.at("/content/0/toolName").asText()).isEqualTo("get_current_trip");

        JsonNode usage = get("/api/admin/ai-usage", admin, 200).get("data");
        assertThat(usage.get("assistantTurns").asLong()).isPositive();
        assertThat(usage.get("topTools")).isNotEmpty();

        JsonNode trips = get("/api/admin/trips?size=5", admin, 200).get("data");
        assertThat(trips.at("/content/0").has("activityCount")).isTrue();
        assertThat(trips.at("/content/0").has("name")).isFalse();
        assertThat(get("/api/admin/users?size=5", admin, 200).at("/data/content/0").has("tripCount")).isTrue();
    }

    @Test
    @DisplayName("Giới hạn chung 120 request/phút/người: vượt thì 429 kèm retryAfterSeconds")
    void apiRateLimit() throws Exception {
        String token = registerUser();
        JsonNode limited = null;
        for (int i = 0; i < 250 && limited == null; i++) {
            var result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .get("/api/me/preferences").header("Authorization", "Bearer " + token)).andReturn();
            if (result.getResponse().getStatus() == 429) {
                limited = objectMapper.readTree(result.getResponse().getContentAsString());
            }
        }
        assertThat(limited).isNotNull();
        assertThat(limited.get("code").asText()).isEqualTo("RATE_LIMITED");
        assertThat(limited.at("/details/retryAfterSeconds").asLong()).isBetween(1L, 60L);
    }
}
