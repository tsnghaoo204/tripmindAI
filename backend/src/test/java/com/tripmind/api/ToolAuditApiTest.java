package com.tripmind.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tripmind.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** BR-505: mọi lượt gọi công cụ đều để lại dòng nhật ký, kể cả khi công cụ hay mô hình lỗi. */
class ToolAuditApiTest extends IntegrationTest {

    private String token;
    private long tripId;

    @BeforeEach
    void setUp() throws Exception {
        llm.reset();
        token = registerUser();
        LocalDate start = LocalDate.now().plusDays(6);
        tripId = createTrip(token, start.toString(), start.toString()).get("id").asLong();
    }

    @Test
    @DisplayName("Công cụ ném lỗi vẫn ghi ai_tool_executions trạng thái ERROR, lượt hỏi vẫn kết thúc bình thường")
    void failingToolIsAudited() throws Exception {
        llm.thenToolCalls(Map.of("optimize_day_order", "{\"dayNumber\":99}"))
                .thenToolCalls(Map.of("no_such_tool", "{}"))
                .thenText("Không sắp lại được.");

        String stream = chat(token, tripId, Map.of("message", "Sắp lại ngày 99"));

        assertThat(stream).contains("event:done").doesNotContain("event:error");
        assertThat(eventData(stream, "tool_end").get("status").asText()).isEqualTo("ERROR");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT tool_name, status, error_message, user_id, message_id, result::text AS result "
                        + "FROM ai_tool_executions WHERE trip_id = ? ORDER BY id", tripId);
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).containsEntry("tool_name", "optimize_day_order").containsEntry("status", "ERROR");
        assertThat((String) rows.get(0).get("error_message")).contains("99");
        assertThat((String) rows.get(0).get("result")).contains("error");
        assertThat(rows.get(1)).containsEntry("tool_name", "no_such_tool").containsEntry("status", "ERROR");
        assertThat(rows).allSatisfy(r -> {
            assertThat(r.get("user_id")).isNotNull();
            assertThat(r.get("message_id")).isNotNull();
        });

        JsonNode activity = get("/api/trips/" + tripId + "/ai/activity", token, 200).get("data");
        assertThat(activity.findValues("status")).extracting(JsonNode::asText).containsOnly("ERROR");
    }

    @Test
    @DisplayName("Mô hình ném lỗi giữa lượt: luồng SSE gửi event:error rồi đóng, công cụ đã chạy vẫn được ghi")
    void llmFailureMidTurnEmitsErrorAndCloses() throws Exception {
        llm.thenToolCalls(Map.of("get_current_trip", "{}"))
                .thenThrow(new IllegalStateException("upstream 500"));

        MvcResult result = mockMvc.perform(post("/api/trips/" + tripId + "/ai/chat")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("message", "Chuyến của tôi?"))))
                .andReturn();
        assertThat(result.getRequest().isAsyncStarted()).isTrue();
        // getAsyncResult chỉ trả về khi emitter đã complete, quá hạn thì ném lỗi
        result.getAsyncResult(20_000);
        String stream = result.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(stream).contains("event:error").doesNotContain("event:done").doesNotContain("upstream 500");
        assertThat(eventData(stream, "error").get("code").asText()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM ai_tool_executions WHERE trip_id = ? AND tool_name = 'get_current_trip'",
                String.class, tripId)).isEqualTo("OK");

        // Lượt sau vẫn chạy được
        llm.thenText("Đã ổn.");
        assertThat(chat(token, tripId, Map.of("message", "Thử lại"))).contains("event:done");
    }
}
