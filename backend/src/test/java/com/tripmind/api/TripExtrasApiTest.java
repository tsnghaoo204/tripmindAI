package com.tripmind.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tripmind.entities.PlaceEntity;
import com.tripmind.enums.PlaceAdoption;
import com.tripmind.repositories.PlaceRepository;
import com.tripmind.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class TripExtrasApiTest extends IntegrationTest {

    @Autowired
    private PlaceRepository placeRepository;

    private final LocalDate start = LocalDate.now().plusDays(7);
    private String token;
    private long tripId;

    @BeforeEach
    void setUp() throws Exception {
        llm.reset();
        token = registerUser();
        tripId = createTrip(token, start.toString(), start.plusDays(1).toString()).get("id").asLong();
    }

    @Test
    @DisplayName("Checklist: gợi ý từ bộ luật (thời tiết lỗi thì bỏ qua và báo), thêm, bỏ trùng, tick, xoá")
    void checklist() throws Exception {
        JsonNode suggestions = get("/api/trips/" + tripId + "/checklist/suggestions", token, 200).get("data");
        assertThat(suggestions.get("skippedRules")).extracting(JsonNode::asText).containsExactly("WEATHER");
        assertThat(titles(suggestions.get("items"))).contains("Giấy tờ tùy thân");

        JsonNode created = call(post("/api/trips/" + tripId + "/checklist"), token, List.of(
                Map.of("kind", "PACK", "title", "Giấy tờ tùy thân", "source", "SUGGESTED", "reason", "Luôn cần mang theo"),
                Map.of("kind", "PACK", "title", "giấy tờ tùy thân"),
                Map.of("kind", "TODO", "title", "Đặt xe ra sân bay", "dueDate", start.minusDays(1).toString())), 201).get("data");
        assertThat(created).hasSize(2);

        JsonNode again = get("/api/trips/" + tripId + "/checklist/suggestions", token, 200).get("data");
        assertThat(titles(again.get("items"))).doesNotContain("Giấy tờ tùy thân");

        long itemId = created.get(0).get("id").asLong();
        assertThat(call(patch("/api/checklist-items/" + itemId), token, Map.of("done", true), 200).at("/data/done")
                .asBoolean()).isTrue();
        call(patch("/api/checklist-items/" + itemId), registerUser(), Map.of("done", false), 404);
        call(delete("/api/checklist-items/" + itemId), token, null, 200);
        assertThat(get("/api/trips/" + tripId + "/checklist?kind=PACK", token, 200).get("data")).isEmpty();
    }

    @Test
    @DisplayName("Đánh giá chỉ mở khi chuyến đã xong; chỗ đã chê bị search_places lọc bỏ ở chuyến sau")
    void ratingsFeedSearch() throws Exception {
        String name = "Quan Chao " + UUID.randomUUID().toString().substring(0, 6);
        PlaceEntity place = placeRepository.save(PlaceEntity.builder().provider("GOOGLE").externalId("seed-" + UUID.randomUUID())
                .name(name).category("restaurant").latitude(new BigDecimal("16.06")).longitude(new BigDecimal("108.24"))
                .adoptedVia(PlaceAdoption.SAVED).build());
        call(post("/api/trips/" + tripId + "/itinerary/activities"), token,
                Map.of("dayNumber", 1, "title", "An sang", "activityType", "FOOD", "placeId", place.getId()), 201);
        String ratingUrl = "/api/trips/" + tripId + "/places/" + place.getId() + "/rating";

        assertThat(call(put(ratingUrl), token, Map.of("verdict", "DISLIKE"), 409).get("code").asText())
                .isEqualTo("TRIP_NOT_ENDED");
        call(put("/api/trips/" + tripId + "/phase"), token, Map.of("phase", "AFTER"), 200);
        assertThat(get("/api/trips/" + tripId + "/review/places", token, 200).at("/data/0/place/name").asText()).isEqualTo(name);
        call(put(ratingUrl), token, Map.of("verdict", "DISLIKE", "note", "Phục vụ chậm"), 200);
        assertThat(get("/api/me/place-ratings", token, 200).at("/data/0/verdict").asText()).isEqualTo("DISLIKE");

        long nextTrip = createTrip(token, start.plusDays(30).toString(), start.plusDays(30).toString()).get("id").asLong();
        llm.thenToolCalls(Map.of("search_places", objectMapper.writeValueAsString(Map.of("query", name))))
                .thenText("Không còn chỗ nào phù hợp.");
        chat(token, nextTrip, Map.of("message", "Gợi ý quán ăn sáng"));
        String toolResult = llm.prompts().get(1).getInstructions().stream()
                .filter(m -> m instanceof org.springframework.ai.chat.messages.ToolResponseMessage)
                .map(m -> ((org.springframework.ai.chat.messages.ToolResponseMessage) m).getResponses().get(0).responseData())
                .findFirst().orElseThrow();
        JsonNode result = objectMapper.readTree(toolResult);
        assertThat(result.get("places")).isEmpty();
        assertThat(result.get("hiddenDislikedPlaces").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("Xuất .ics và nhân bản chuyến: giữ hoạt động + checklist, bỏ chi tiêu, báo hoạt động từng bị bỏ")
    void exportAndDuplicate() throws Exception {
        long act = call(post("/api/trips/" + tripId + "/itinerary/activities"), token, Map.of("dayNumber", 1,
                "title", "Ngắm hoàng hôn", "activityType", "SIGHTSEEING", "startTime", "17:00"), 201).at("/data/id").asLong();
        call(post("/api/trips/" + tripId + "/itinerary/activities"), token, Map.of("dayNumber", 2,
                "title", "Tour đảo", "activityType", "SIGHTSEEING"), 201);
        long skipped = call(post("/api/trips/" + tripId + "/itinerary/activities"), token, Map.of("dayNumber", 2,
                "title", "Lặn biển", "activityType", "SIGHTSEEING"), 201).at("/data/id").asLong();
        call(put("/api/activities/" + skipped), token, Map.of("status", "SKIPPED", "skipReason", "RAIN"), 200);
        call(post("/api/trips/" + tripId + "/checklist"), token, List.of(Map.of("kind", "TODO", "title", "Đặt tour",
                "dueDate", start.minusDays(2).toString())), 201);
        call(post("/api/trips/" + tripId + "/checklist"), token, List.of(Map.of("kind", "PACK", "title", "Kính lặn")), 201);
        call(patch("/api/checklist-items/" + get("/api/trips/" + tripId + "/checklist?kind=PACK", token, 200)
                .at("/data/0/id").asLong()), token, Map.of("done", true), 200);
        call(post("/api/trips/" + tripId + "/expenses"), token, Map.of("category", "FOOD", "amount", 100000,
                "expenseDate", start.toString()), 201);

        MvcResult ics = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/trips/" + tripId + "/export.ics").header("Authorization", "Bearer " + token))
                .andReturn();
        assertThat(ics.getResponse().getContentType()).startsWith("text/calendar");
        assertThat(ics.getResponse().getHeader("Content-Disposition")).contains("chuyen-test.ics");
        assertThat(ics.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("UID:activity-" + act + "@tripmind", "SUMMARY:Ngắm hoàng hôn", "Tour đảo → Lặn biển");

        LocalDate newStart = start.plusDays(60);
        JsonNode copy = call(post("/api/trips/" + tripId + "/duplicate"), token,
                Map.of("startDate", newStart.toString()), 201).get("data");
        long newTrip = copy.at("/trip/id").asLong();
        assertThat(copy.get("activitiesCopied").asInt()).isEqualTo(3);
        assertThat(copy.at("/previouslySkipped/0/title").asText()).isEqualTo("Lặn biển");
        assertThat(copy.at("/trip/endDate").asText()).isEqualTo(newStart.plusDays(1).toString());
        assertThat(copy.at("/trip/name").asText()).isEqualTo("Chuyen test (bản sao)");

        JsonNode newChecklist = get("/api/trips/" + newTrip + "/checklist", token, 200).get("data");
        assertThat(newChecklist).hasSize(2);
        assertThat(newChecklist.findValues("done")).allMatch(d -> !d.asBoolean());
        assertThat(newChecklist.findValues("dueDate").get(0).asText()).isEqualTo(newStart.minusDays(2).toString());
        assertThat(get("/api/trips/" + newTrip + "/expenses", token, 200).get("data")).isEmpty();
        JsonNode newItinerary = get("/api/trips/" + newTrip + "/itinerary", token, 200).get("data");
        assertThat(newItinerary.findValues("status")).allMatch(s -> s.asText().equals("PLANNED"));
    }

    private List<String> titles(JsonNode items) {
        return items.findValues("title").stream().map(JsonNode::asText).toList();
    }
}
