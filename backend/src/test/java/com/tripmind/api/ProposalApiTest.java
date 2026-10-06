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
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

class ProposalApiTest extends IntegrationTest {

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private com.tripmind.services.ai.ProposalService proposalService;

    private final LocalDate start = LocalDate.now().plusDays(4);
    private String token;
    private long tripId;
    private long keepId;
    private long dropId;

    @BeforeEach
    void setUp() throws Exception {
        llm.reset();
        token = registerUser();
        tripId = createTrip(token, start.toString(), start.plusDays(1).toString()).get("id").asLong();
        keepId = addActivity("Cau Rong", seedPlace("Cau Rong " + suffix(), 16.0610, 108.2274, null).getId(), 0L);
        dropId = addActivity("Cho Han", seedPlace("Cho Han " + suffix(), 16.0680, 108.2240, null).getId(), 100_000L);
    }

    @Test
    @DisplayName("Đề xuất → xem → áp dụng → áp dụng lần hai 409, dữ liệu không nhân đôi (BR-508)")
    void proposeApplyAndApplyTwice() throws Exception {
        PlaceEntity seafood = seedPlace("Hai san Be Man " + suffix(), 16.0612, 108.2470, 2);
        long proposalId = proposeAddAndRemove(seafood);

        JsonNode proposal = get("/api/proposals/" + proposalId, token, 200).get("data");
        assertThat(proposal.get("status").asText()).isEqualTo("PENDING");
        assertThat(proposal.get("changes")).hasSize(2);
        assertThat(proposal.get("estimatedCostDelta").asLong()).isEqualTo(400_000L - 100_000L);

        // Trợ lý chưa ghi gì vào lịch trình (QĐ-01).
        assertThat(activityTitles()).containsExactly("Cau Rong", "Cho Han");

        JsonNode applied = call(post("/api/trips/" + tripId + "/ai/apply"), token, Map.of("proposalId", proposalId), 200)
                .get("data");
        assertThat(applied.get("applied").asInt()).isEqualTo(2);
        assertThat(activityTitles()).containsExactly("Cau Rong", "An hai san");
        assertThat(jdbc.queryForObject("SELECT created_by FROM activities WHERE from_proposal_id = ?", String.class,
                proposalId)).isEqualTo("AI");

        JsonNode again = call(post("/api/trips/" + tripId + "/ai/apply"), token, Map.of("proposalId", proposalId), 409);
        assertThat(again.get("code").asText()).isEqualTo("PROPOSAL_NOT_PENDING");
        assertThat(activityTitles()).hasSize(2);
    }

    @Test
    @DisplayName("BR-509: mô hình bịa mã địa điểm thì thao tác bị loại, không có đề xuất nào được tạo")
    void madeUpPlaceIsRejected() throws Exception {
        llm.thenToolCalls(Map.of("propose_itinerary_changes", json(Map.of(
                        "summary", "Them quan", "reason", "gan",
                        "changes", List.of(Map.of("op", "ADD", "dayNumber", 1, "title", "An toi",
                                "activityType", "FOOD", "placeExternalId", "made-up-" + suffix(), "reason", "x"))))))
                .thenText("Không tìm được chỗ phù hợp.");

        String stream = chat(token, tripId, Map.of("message", "Thêm quán ăn tối"));

        assertThat(stream).doesNotContain("event:proposal");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_proposals WHERE trip_id = ?", Integer.class, tripId))
                .isZero();
        String toolResult = jdbc.queryForObject(
                "SELECT result::text FROM ai_tool_executions WHERE trip_id = ? AND tool_name = 'propose_itinerary_changes'",
                String.class, tripId);
        assertThat(toolResult).contains("PLACE_NOT_VERIFIED");
    }

    @Test
    @DisplayName("BR-506 + BR-507: chỉ nhận mã đề xuất; sai chuyến, sai người, đã từ chối, quá hạn đều bị chặn")
    void applyChecks() throws Exception {
        long proposalId = proposeAddAndRemove(seedPlace("Quan " + suffix(), 16.06, 108.24, 1));

        JsonNode noId = call(post("/api/trips/" + tripId + "/ai/apply"), token,
                Map.of("changes", List.of(Map.of("op", "REMOVE", "activityId", keepId))), 400);
        assertThat(noId.get("code").asText()).isEqualTo("VALIDATION_ERROR");

        long otherTrip = createTrip(token, start.toString(), start.toString()).get("id").asLong();
        assertThat(call(post("/api/trips/" + otherTrip + "/ai/apply"), token, Map.of("proposalId", proposalId), 404)
                .get("code").asText()).isEqualTo("PROPOSAL_NOT_FOUND");
        call(post("/api/trips/" + tripId + "/ai/apply"), registerUser(), Map.of("proposalId", proposalId), 404);
        get("/api/proposals/" + proposalId, registerUser(), 404);

        jdbc.update("UPDATE ai_proposals SET expires_at = now() - interval '1 minute' WHERE id = ?", proposalId);
        assertThat(call(post("/api/trips/" + tripId + "/ai/apply"), token, Map.of("proposalId", proposalId), 409)
                .get("code").asText()).isEqualTo("PROPOSAL_EXPIRED");

        jdbc.update("UPDATE ai_proposals SET expires_at = now() + interval '10 minute' WHERE id = ?", proposalId);
        call(post("/api/proposals/" + proposalId + "/reject"), token, null, 200);
        assertThat(call(post("/api/trips/" + tripId + "/ai/apply"), token, Map.of("proposalId", proposalId), 409)
                .get("code").asText()).isEqualTo("PROPOSAL_NOT_PENDING");
        assertThat(activityTitles()).containsExactly("Cau Rong", "Cho Han");
    }

    @Test
    @DisplayName("Hoàn tác giữ phần người dùng đã sửa tay, dựng lại hoạt động đã xoá; xem trước nói đúng việc sắp xảy ra")
    void undoKeepsManualEdits() throws Exception {
        PlaceEntity seafood = seedPlace("Hai san " + suffix(), 16.0612, 108.2470, 2);
        long proposalId = proposeAddAndRemove(seafood);
        call(post("/api/trips/" + tripId + "/ai/apply"), token, Map.of("proposalId", proposalId), 200);
        long addedId = jdbc.queryForObject("SELECT id FROM activities WHERE from_proposal_id = ?", Long.class, proposalId);

        JsonNode explanation = get("/api/activities/" + addedId + "/explanation", token, 200).get("data");
        assertThat(explanation.get("known").asBoolean()).isTrue();
        assertThat(explanation.get("changeReason").asText()).isEqualTo("Gần biển, mức giá vừa");
        assertThat(explanation.get("tools")).isNotEmpty();
        assertThat(get("/api/activities/" + keepId + "/explanation", token, 200).at("/data/unknownReason").asText())
                .isEqualTo("CREATED_BY_USER");

        // Người dùng sửa tay hoạt động AI vừa thêm.
        call(put("/api/activities/" + addedId), token, Map.of("notes", "Đặt bàn trước"), 200);

        JsonNode preview = get("/api/trips/" + tripId + "/ai/undo/" + proposalId + "/preview", token, 200).get("data");
        assertThat(preview.get("willRestore")).extracting(JsonNode::asText).containsExactly("Cho Han");
        assertThat(preview.get("willDelete")).isEmpty();
        assertThat(preview.at("/undoSkipped/0/reason").asText()).isEqualTo("ACTIVITY_EDITED_AFTER_APPLY");
        assertThat(activityTitles()).containsExactly("Cau Rong", "An hai san");

        JsonNode undone = call(post("/api/trips/" + tripId + "/ai/undo"), token, Map.of("proposalId", proposalId), 200)
                .get("data");
        assertThat(undone.get("reverted").asInt()).isEqualTo(1);
        assertThat(activityTitles()).containsExactlyInAnyOrder("Cau Rong", "Cho Han", "An hai san");
        assertThat(activityTitles().get(1)).isEqualTo("Cho Han");

        assertThat(call(post("/api/trips/" + tripId + "/ai/undo"), token, Map.of("proposalId", proposalId), 409)
                .get("code").asText()).isEqualTo("PROPOSAL_NOT_APPLIED");
    }

    @Test
    @DisplayName("Hoàn tác quá 10 phút bị từ chối UNDO_WINDOW_CLOSED")
    void undoWindowCloses() throws Exception {
        long proposalId = proposeAddAndRemove(seedPlace("Quan " + suffix(), 16.06, 108.24, 1));
        call(post("/api/trips/" + tripId + "/ai/apply"), token, Map.of("proposalId", proposalId), 200);
        jdbc.update("UPDATE ai_proposals SET applied_at = now() - interval '11 minute' WHERE id = ?", proposalId);

        assertThat(call(post("/api/trips/" + tripId + "/ai/undo"), token, Map.of("proposalId", proposalId), 409)
                .get("code").asText()).isEqualTo("UNDO_WINDOW_CLOSED");
    }

    @Test
    @DisplayName("Sắp lại theo optimize_day_order; ngày đổi sau khi dựng đề xuất thì áp dụng bị huỷ PROPOSAL_STALE")
    void reorderBecomesStale() throws Exception {
        llm.thenToolCalls(Map.of("optimize_day_order", "{\"dayNumber\":1}"))
                .thenToolCalls(Map.of("propose_itinerary_changes", json(Map.of(
                        "summary", "Doi thu tu", "reason", "ngan hon",
                        "changes", List.of(Map.of("op", "REORDER", "dayNumber", 1,
                                "activityIds", List.of(dropId, keepId), "reason", "di cho truoc"))))))
                .thenText("Mình đã đề xuất đổi thứ tự.");
        String stream = chat(token, tripId, Map.of("message", "Sắp lại ngày 1"));
        long proposalId = eventData(stream, "proposal").get("proposalId").asLong();

        addActivity("Them tay", null, null);
        JsonNode stale = call(post("/api/trips/" + tripId + "/ai/apply"), token, Map.of("proposalId", proposalId), 409);
        assertThat(stale.get("code").asText()).isEqualTo("PROPOSAL_STALE");
        assertThat(activityTitles()).containsExactly("Cau Rong", "Cho Han", "Them tay");
    }

    @Test
    @DisplayName("LIFO có nới: đề xuất sau đụng cùng hoạt động thì chặn hoàn tác đề xuất trước; đề xuất quá hạn bị dọn")
    void undoBlockedByLaterProposalAndExpiry() throws Exception {
        long first = proposeUpdate("Ghi chu A");
        call(post("/api/trips/" + tripId + "/ai/apply"), token, Map.of("proposalId", first), 200);
        long second = proposeUpdate("Ghi chu B");
        call(post("/api/trips/" + tripId + "/ai/apply"), token, Map.of("proposalId", second), 200);

        JsonNode blocked = call(post("/api/trips/" + tripId + "/ai/undo"), token, Map.of("proposalId", first), 409);
        assertThat(blocked.get("code").asText()).isEqualTo("UNDO_BLOCKED_BY_LATER");
        assertThat(blocked.at("/details/blockedBy/0").asLong()).isEqualTo(second);

        call(post("/api/trips/" + tripId + "/ai/undo"), token, Map.of("proposalId", second), 200);
        call(post("/api/trips/" + tripId + "/ai/undo"), token, Map.of("proposalId", first), 200);
        assertThat(jdbc.queryForObject("SELECT notes FROM activities WHERE id = ?", String.class, keepId)).isNull();

        long pending = proposeUpdate("Ghi chu C");
        jdbc.update("UPDATE ai_proposals SET expires_at = now() - interval '1 minute' WHERE id = ?", pending);
        proposalService.expirePending();
        assertThat(get("/api/proposals/" + pending, token, 200).at("/data/status").asText()).isEqualTo("EXPIRED");
    }

    private long proposeUpdate(String notes) throws Exception {
        llm.thenToolCalls(Map.of("propose_itinerary_changes", json(Map.of("summary", notes, "reason", "r",
                        "changes", List.of(Map.of("op", "UPDATE", "activityId", keepId, "notes", notes, "reason", "r"))))))
                .thenText("Xong");
        return eventData(chat(token, tripId, Map.of("message", notes)), "proposal").get("proposalId").asLong();
    }

    /** Trợ lý tìm quán rồi đề xuất thêm quán đó vào ngày 1 và bỏ "Cho Han". */
    private long proposeAddAndRemove(PlaceEntity place) throws Exception {
        llm.thenToolCalls(Map.of("search_places", json(Map.of("query", place.getName()))))
                .thenToolCalls(Map.of("propose_itinerary_changes", json(Map.of(
                        "summary", "Đổi chợ thành bữa hải sản",
                        "reason", "Gần biển hơn",
                        "changes", List.of(
                                Map.of("op", "ADD", "dayNumber", 1, "title", "An hai san", "activityType", "FOOD",
                                        "placeExternalId", place.getExternalId(), "reason", "Gần biển, mức giá vừa"),
                                Map.of("op", "REMOVE", "activityId", dropId, "reason", "Đi chợ hôm khác"))))))
                .thenText("Mình đã dựng đề xuất, bạn xem và áp dụng nhé.");
        String stream = chat(token, tripId, Map.of("message", "Gợi ý quán hải sản thay cho chợ"));
        JsonNode proposal = eventData(stream, "proposal");
        assertThat(proposal).as(stream).isNotNull();
        return proposal.get("proposalId").asLong();
    }

    private long addActivity(String title, Long placeId, Long cost) throws Exception {
        Map<String, Object> body = new java.util.HashMap<>(Map.of("dayNumber", 1, "title", title, "activityType", "SIGHTSEEING"));
        if (placeId != null) {
            body.put("placeId", placeId);
        }
        if (cost != null) {
            body.put("estimatedCost", cost);
        }
        return call(post("/api/trips/" + tripId + "/itinerary/activities"), token, body, 201).at("/data/id").asLong();
    }

    private List<String> activityTitles() throws Exception {
        JsonNode activities = get("/api/trips/" + tripId + "/itinerary", token, 200).at("/data/days/0/activities");
        return java.util.stream.StreamSupport.stream(activities.spliterator(), false)
                .map(a -> a.get("title").asText()).toList();
    }

    private PlaceEntity seedPlace(String name, double lat, double lng, Integer priceLevel) {
        return placeRepository.save(PlaceEntity.builder()
                .provider("GOOGLE")
                .externalId("seed-" + UUID.randomUUID())
                .name(name)
                .category("restaurant")
                .latitude(BigDecimal.valueOf(lat))
                .longitude(BigDecimal.valueOf(lng))
                .priceLevel(priceLevel == null ? null : priceLevel.shortValue())
                .adoptedVia(PlaceAdoption.SAVED)
                .build());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
