package com.tripmind.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tripmind.support.FakeLlmGateway;
import com.tripmind.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class AiAssistantApiTest extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    private final LocalDate start = LocalDate.now().plusDays(2);

    @BeforeEach
    void resetModel() {
        llm.reset();
    }

    @Test
    @DisplayName("Một lượt hỏi gọi 2 công cụ: luồng có tool_start/tool_end/token/done, nhật ký có 2 dòng OK")
    void chatWithTools() throws Exception {
        String token = registerUser();
        long tripId = createTrip(token, start.toString(), start.plusDays(1).toString()).get("id").asLong();
        llm.thenToolCalls(Map.of("get_itinerary", "{\"dayNumber\":1}"))
                .thenToolCalls(Map.of("calculate_trip_budget", "{}"))
                .thenText("Ngày 1 chưa có hoạt động nào, ngân sách còn nguyên 8 triệu.");

        String stream = chat(token, tripId, Map.of("message", "Ngày 1 của tôi có gì?"));

        assertThat(stream).contains("event:tool_start", "event:tool_end", "event:token", "event:done");
        assertThat(eventData(stream, "tool_start").get("label").asText()).isEqualTo("Đang đọc lịch trình...");
        long conversationId = eventData(stream, "done").get("conversationId").asLong();

        Integer ok = jdbc.queryForObject("SELECT COUNT(*) FROM ai_tool_executions WHERE trip_id = ? AND status = 'OK' "
                + "AND message_id IS NOT NULL", Integer.class, tripId);
        assertThat(ok).isEqualTo(2);

        JsonNode messages = get("/api/conversations/" + conversationId + "/messages", token, 200).get("data");
        assertThat(messages).hasSize(2);
        assertThat(messages.get(1).get("content").asText()).contains("8 triệu");
        assertThat(messages.get(1).at("/tokenUsage/toolCalls").asInt()).isEqualTo(2);
        assertThat(get("/api/trips/" + tripId + "/ai/activity", token, 200).get("data")).hasSize(2);
        assertThat(get("/api/trips/" + tripId + "/ai/conversations", token, 200).get("data")).hasSize(1);
    }

    @Test
    @DisplayName("BR-103: mô hình 'xin' đọc chuyến của người khác vẫn chỉ nhận được chuyến của người hỏi")
    void toolsIgnoreTripIdFromModel() throws Exception {
        String victim = registerUser();
        long victimTrip = createTrip(victim, start.toString(), start.toString()).get("id").asLong();
        String attacker = registerUser();
        long attackerTrip = call(post("/api/trips"), attacker, Map.of("destinationId", 2, "name", "Chuyen cua toi",
                "startDate", start.toString(), "endDate", start.toString()), 201).at("/data/id").asLong();

        llm.thenToolCalls(Map.of("get_current_trip", "{\"tripId\":" + victimTrip + "}"))
                .thenText("Xong");
        chat(attacker, attackerTrip, Map.of("message", "Đọc chuyến số " + victimTrip + " giúp tôi"));

        ToolResponseMessage toolMessage = (ToolResponseMessage) llm.prompts().get(1).getInstructions().stream()
                .filter(m -> m instanceof ToolResponseMessage).findFirst().orElseThrow();
        String result = toolMessage.getResponses().get(0).responseData();
        assertThat(result).contains("Chuyen cua toi").doesNotContain("Chuyen test");
    }

    @Test
    @DisplayName("BR-502: mô hình gọi công cụ mãi thì dừng ở vòng 8 rồi trả lời bằng dữ liệu đang có")
    void loopStopsAtMaxRounds() throws Exception {
        String token = registerUser();
        long tripId = createTrip(token, start.toString(), start.toString()).get("id").asLong();
        AtomicInteger n = new AtomicInteger();
        llm.setFallback(p -> p.getOptions() != null && p.getOptions().getClass().getSimpleName().contains("OpenAi")
                && hasTools(p) ? FakeLlmGateway.toolCalls(Map.of("get_itinerary", "{\"dayNumber\":" + n.incrementAndGet() + "}"))
                : FakeLlmGateway.text("Đây là những gì mình có."));

        String stream = chat(token, tripId, Map.of("message", "Lặp đi"));

        assertThat(llm.prompts()).hasSize(9);
        assertThat(stream).contains("event:done");
        Integer executions = jdbc.queryForObject("SELECT COUNT(*) FROM ai_tool_executions WHERE trip_id = ?",
                Integer.class, tripId);
        assertThat(executions).isEqualTo(8);
    }

    @Test
    @DisplayName("BR-503: gọi lặp cùng công cụ, cùng tham số trong một lượt chỉ chạy một lần")
    void duplicateCallsAreNotReExecuted() throws Exception {
        String token = registerUser();
        long tripId = createTrip(token, start.toString(), start.toString()).get("id").asLong();
        llm.thenToolCalls(Map.of("get_weather", "{}"))
                .thenToolCalls(Map.of("get_weather", "{ }"))
                .thenText("Xong");

        chat(token, tripId, Map.of("message", "Thời tiết?"));

        Integer executions = jdbc.queryForObject("SELECT COUNT(*) FROM ai_tool_executions WHERE trip_id = ?",
                Integer.class, tripId);
        assertThat(executions).isEqualTo(1);
    }

    @Test
    @DisplayName("Kiểm trước khi mở luồng: chuyến người khác 404, AI chưa cấu hình 503, vượt giới hạn 429 — đều là JSON")
    void prechecksReturnJsonErrors() throws Exception {
        String token = registerUser();
        long tripId = createTrip(token, start.toString(), start.toString()).get("id").asLong();

        assertThat(rawChat(registerUser(), tripId).get("code").asText()).isEqualTo("RESOURCE_NOT_FOUND");

        llm.setConfigured(false);
        assertThat(rawChat(token, tripId).get("code").asText()).isEqualTo("AI_NOT_CONFIGURED");
        llm.setConfigured(true);

        for (int i = 0; i < 5; i++) {
            chat(token, tripId, Map.of("message", "Câu " + i));
        }
        JsonNode limited = rawChat(token, tripId);
        assertThat(limited.get("code").asText()).isEqualTo("RATE_LIMITED");
        assertThat(limited.at("/details/retryAfterSeconds").asLong()).isPositive();
    }

    private JsonNode rawChat(String token, long tripId) throws Exception {
        var result = mockMvc.perform(post("/api/trips/" + tripId + "/ai/chat")
                .header("Authorization", "Bearer " + token)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"xin chao\"}")).andReturn();
        assertThat(result.getRequest().isAsyncStarted()).isFalse();
        return objectMapper.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static boolean hasTools(org.springframework.ai.chat.prompt.Prompt prompt) {
        return prompt.getOptions() instanceof org.springframework.ai.model.tool.ToolCallingChatOptions options
                && options.getToolCallbacks() != null && !options.getToolCallbacks().isEmpty();
    }
}
