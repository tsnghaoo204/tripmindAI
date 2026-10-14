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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * BR-101/BR-104: người dùng B chạm vào dữ liệu của A qua mọi đường ghi đều nhận 404,
 * và dữ liệu của A giữ nguyên.
 */
class CrossUserAccessApiTest extends IntegrationTest {

    @Autowired
    private PlaceRepository placeRepository;

    private final LocalDate start = LocalDate.now().plusDays(5);
    private String alice;
    private String bob;
    private long aTrip;
    private long aDay;
    private long aActivity;
    private long aPlace;
    private long bTrip;
    private long bActivity;

    @BeforeEach
    void setUp() throws Exception {
        llm.reset();
        alice = registerUser();
        bob = registerUser();
        aTrip = createTrip(alice, start.toString(), start.plusDays(1).toString()).get("id").asLong();
        aPlace = seedPlace("Quan A " + UUID.randomUUID().toString().substring(0, 6)).getId();
        aActivity = call(post("/api/trips/" + aTrip + "/itinerary/activities"), alice, Map.of(
                "dayNumber", 1, "title", "An trua A", "activityType", "FOOD",
                "estimatedCost", 150000, "placeId", aPlace), 201).at("/data/id").asLong();
        aDay = get("/api/trips/" + aTrip + "/itinerary", alice, 200).at("/data/days/0/id").asLong();

        bTrip = createTrip(bob, start.toString(), start.toString()).get("id").asLong();
        bActivity = call(post("/api/trips/" + bTrip + "/itinerary/activities"), bob, Map.of(
                "dayNumber", 1, "title", "An trua B", "activityType", "FOOD", "estimatedCost", 150000), 201)
                .at("/data/id").asLong();
    }

    @Test
    @DisplayName("Hoạt động, ngày, phương án thay thế của A: B sửa/xoá/đổi trạng thái/chọn đều 404")
    void activitiesAndDays() throws Exception {
        call(put("/api/activities/" + aActivity), bob, Map.of("title", "Hack"), 404);
        call(put("/api/activities/" + aActivity), bob, Map.of("status", "DONE"), 404);
        call(put("/api/activities/" + aActivity), bob, Map.of("status", "SKIPPED", "skipReason", "RAIN"), 404);
        call(delete("/api/activities/" + aActivity), bob, null, 404);
        call(post("/api/trips/" + aTrip + "/itinerary/activities"), bob,
                Map.of("dayNumber", 1, "title", "Chen vao", "activityType", "SIGHTSEEING"), 404);
        call(put("/api/itinerary-days/" + aDay + "/reorder"), bob, Map.of("activityIds", List.of(aActivity)), 404);
        get("/api/activities/" + aActivity + "/explanation", bob, 404);

        // Ngày của B không được kéo hoạt động của A vào
        long bDay = get("/api/trips/" + bTrip + "/itinerary", bob, 200).at("/data/days/0/id").asLong();
        call(put("/api/itinerary-days/" + bDay + "/reorder"), bob,
                Map.of("activityIds", List.of(bActivity, aActivity)), 422);

        JsonNode aAlts = get("/api/activities/" + aActivity + "/alternatives", alice, 200).get("data");
        long aAltId = aAlts.get(0).get("id").asLong();
        get("/api/activities/" + aActivity + "/alternatives", bob, 404);
        call(put("/api/activities/" + aActivity + "/alternatives/" + aAltId + "/select"), bob, null, 404);
        // Phương án của A gắn vào hoạt động của B
        call(put("/api/activities/" + bActivity + "/alternatives/" + aAltId + "/select"), bob, null, 404);

        // Phương án của một hoạt động khác trong chính chuyến của B
        long bOther = call(post("/api/trips/" + bTrip + "/itinerary/activities"), bob, Map.of(
                "dayNumber", 1, "title", "An toi B", "activityType", "FOOD", "estimatedCost", 300000), 201)
                .at("/data/id").asLong();
        get("/api/activities/" + bActivity + "/alternatives", bob, 200);
        long otherAlt = get("/api/activities/" + bOther + "/alternatives", bob, 200).at("/data/0/id").asLong();
        call(put("/api/activities/" + bActivity + "/alternatives/" + otherAlt + "/select"), bob, null, 404);
        assertThat(jdbcTemplate.queryForObject("SELECT title FROM activities WHERE id = ?", String.class, bActivity))
                .isEqualTo("An trua B");

        JsonNode activity = get("/api/trips/" + aTrip + "/itinerary", alice, 200).at("/data/days/0/activities/0");
        assertThat(activity.get("id").asLong()).isEqualTo(aActivity);
        assertThat(activity.get("title").asText()).isEqualTo("An trua A");
        assertThat(activity.get("status").asText()).isEqualTo("PLANNED");
        assertThat(activity.get("estimatedCost").asLong()).isEqualTo(150_000L);
        assertThat(get("/api/activities/" + aActivity + "/alternatives", alice, 200).get("data"))
                .isEqualTo(aAlts);
    }

    @Test
    @DisplayName("Chi tiêu, checklist, đánh giá, địa điểm đã lưu của A: B không sửa/xoá được")
    void expensesChecklistRatingsSavedPlaces() throws Exception {
        long expenseId = call(post("/api/trips/" + aTrip + "/expenses"), alice, Map.of(
                "category", "FOOD", "amount", 450000, "expenseDate", start.toString(), "activityId", aActivity), 201)
                .at("/data/id").asLong();
        Map<String, Object> edit = Map.of("category", "OTHER", "amount", 1, "expenseDate", start.toString());
        call(put("/api/expenses/" + expenseId), bob, edit, 404);
        call(delete("/api/expenses/" + expenseId), bob, null, 404);
        call(post("/api/trips/" + aTrip + "/expenses"), bob, edit, 404);
        // Chi tiêu của B không được gắn vào hoạt động của A
        call(post("/api/trips/" + bTrip + "/expenses"), bob, Map.of(
                "category", "FOOD", "amount", 1, "expenseDate", start.toString(), "activityId", aActivity), 404);

        long bExpense = call(post("/api/trips/" + bTrip + "/expenses"), bob, edit, 201).at("/data/id").asLong();
        call(put("/api/expenses/" + bExpense), bob, Map.of(
                "category", "FOOD", "amount", 1, "expenseDate", start.toString(), "activityId", aActivity), 404);

        long itemId = call(post("/api/trips/" + aTrip + "/checklist"), alice,
                List.of(Map.of("kind", "PACK", "title", "Ao mua")), 201).at("/data/0/id").asLong();
        call(patch("/api/checklist-items/" + itemId), bob, Map.of("done", true), 404);
        call(delete("/api/checklist-items/" + itemId), bob, null, 404);
        call(post("/api/trips/" + aTrip + "/checklist"), bob, List.of(Map.of("kind", "PACK", "title", "X")), 404);

        call(put("/api/trips/" + aTrip + "/phase"), alice, Map.of("phase", "AFTER"), 200);
        String ratingUrl = "/api/trips/" + aTrip + "/places/" + aPlace + "/rating";
        call(put(ratingUrl), alice, Map.of("verdict", "LIKE"), 200);
        call(put(ratingUrl), bob, Map.of("verdict", "DISLIKE"), 404);
        call(delete(ratingUrl), bob, null, 404);
        get("/api/trips/" + aTrip + "/review/places", bob, 404);

        call(post("/api/places/" + aPlace + "/save"), alice, null, 201);
        call(delete("/api/places/" + aPlace + "/save"), bob, null, 200);

        JsonNode expenses = get("/api/trips/" + aTrip + "/expenses", alice, 200).get("data");
        assertThat(expenses).hasSize(1);
        assertThat(expenses.at("/0/amount").asLong()).isEqualTo(450_000L);
        assertThat(expenses.at("/0/category").asText()).isEqualTo("FOOD");
        JsonNode bExpenses = get("/api/trips/" + bTrip + "/expenses", bob, 200).get("data");
        assertThat(bExpenses).as(bExpenses.toString()).hasSize(1);
        assertThat(bExpenses.at("/0/activityId").asLong(0)).isZero();
        assertThat(bExpenses.at("/0/amount").asLong()).isEqualTo(1L);
        JsonNode checklist = get("/api/trips/" + aTrip + "/checklist", alice, 200).get("data");
        assertThat(checklist).hasSize(1);
        assertThat(checklist.at("/0/done").asBoolean()).isFalse();
        assertThat(get("/api/me/place-ratings", alice, 200).at("/data/0/verdict").asText()).isEqualTo("LIKE");
        assertThat(get("/api/me/place-ratings", bob, 200).get("data")).isEmpty();
        assertThat(get("/api/me/saved-places", alice, 200).get("data")).hasSize(1);
    }

    @Test
    @DisplayName("Chuyến của A: B xuất .ics, nhân bản, sửa, đổi giai đoạn, xoá, xem ngân sách/thời tiết đều 404")
    void tripLevel() throws Exception {
        get("/api/trips/" + aTrip, bob, 404);
        get("/api/trips/" + aTrip + "/export.ics", bob, 404);
        call(post("/api/trips/" + aTrip + "/duplicate"), bob, Map.of("startDate", start.plusDays(40).toString()), 404);
        call(put("/api/trips/" + aTrip), bob, Map.of("name", "Hack"), 404);
        call(put("/api/trips/" + aTrip + "/phase"), bob, Map.of("phase", "AFTER"), 404);
        get("/api/trips/" + aTrip + "/budget", bob, 404);
        get("/api/trips/" + aTrip + "/weather", bob, 404);
        get("/api/trips/" + aTrip + "/cost-estimate?placeId=" + aPlace, bob, 404);
        get("/api/trips/" + aTrip + "/checklist/suggestions", bob, 404);
        get("/api/trips/" + aTrip + "/ai/conversations", bob, 404);
        get("/api/trips/" + aTrip + "/ai/activity", bob, 404);
        call(post("/api/trips/" + aTrip + "/ai/chat"), bob, Map.of("message", "Xin chao"), 404);
        call(post("/api/trips/" + aTrip + "/ai/generate"), bob, null, 404);
        call(delete("/api/trips/" + aTrip), bob, null, 404);

        JsonNode trip = get("/api/trips/" + aTrip, alice, 200).get("data");
        assertThat(trip.get("name").asText()).isEqualTo("Chuyen test");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trips WHERE user_id = "
                + "(SELECT user_id FROM trips WHERE id = ?)", Integer.class, bTrip)).isEqualTo(1);
    }

    @Test
    @DisplayName("Đề xuất và hội thoại của A: B xem/áp dụng/từ chối/hoàn tác/nối tiếp đều 404")
    void proposalsAndConversations() throws Exception {
        long proposalId = proposeNotes("Ghi chu A");
        long conversationId = get("/api/trips/" + aTrip + "/ai/conversations", alice, 200).at("/data/0/id").asLong();

        get("/api/proposals/" + proposalId, bob, 404);
        call(post("/api/proposals/" + proposalId + "/reject"), bob, null, 404);
        call(post("/api/trips/" + aTrip + "/ai/apply"), bob, Map.of("proposalId", proposalId), 404);
        call(post("/api/trips/" + bTrip + "/ai/apply"), bob, Map.of("proposalId", proposalId), 404);
        get("/api/conversations/" + conversationId + "/messages", bob, 404);
        call(post("/api/trips/" + bTrip + "/ai/chat"), bob,
                Map.of("message", "Tiep", "conversationId", conversationId), 404);
        assertThat(get("/api/proposals/" + proposalId, alice, 200).at("/data/status").asText()).isEqualTo("PENDING");

        call(post("/api/trips/" + aTrip + "/ai/apply"), alice, Map.of("proposalId", proposalId), 200);
        get("/api/trips/" + aTrip + "/ai/undo/" + proposalId + "/preview", bob, 404);
        get("/api/trips/" + bTrip + "/ai/undo/" + proposalId + "/preview", bob, 404);
        call(post("/api/trips/" + aTrip + "/ai/undo"), bob, Map.of("proposalId", proposalId), 404);
        call(post("/api/trips/" + bTrip + "/ai/undo"), bob, Map.of("proposalId", proposalId), 404);

        assertThat(get("/api/proposals/" + proposalId, alice, 200).at("/data/status").asText()).isEqualTo("APPLIED");
        assertThat(jdbcTemplate.queryForObject("SELECT notes FROM activities WHERE id = ?", String.class, aActivity))
                .isEqualTo("Ghi chu A");
    }

    @Test
    @DisplayName("BR-103: trợ lý trong chuyến của B không đề xuất được thay đổi lên hoạt động của A")
    void assistantCannotTouchOtherUsersActivity() throws Exception {
        llm.thenToolCalls(Map.of("propose_itinerary_changes", objectMapper.writeValueAsString(Map.of(
                        "summary", "s", "reason", "r",
                        "changes", List.of(
                                Map.of("op", "UPDATE", "activityId", aActivity, "notes", "Hack", "reason", "r"),
                                Map.of("op", "REMOVE", "activityId", aActivity, "reason", "r"))))))
                .thenText("Xong");
        String stream = chat(bob, bTrip, Map.of("message", "Sua hoat dong"));

        JsonNode proposal = eventData(stream, "proposal");
        if (proposal != null) {
            call(post("/api/trips/" + bTrip + "/ai/apply"), bob,
                    Map.of("proposalId", proposal.get("proposalId").asLong()), 404);
        }
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_proposals WHERE trip_id = ?", Integer.class, bTrip))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT notes FROM activities WHERE id = ?", String.class, aActivity))
                .isNull();
    }

    private long proposeNotes(String notes) throws Exception {
        llm.thenToolCalls(Map.of("propose_itinerary_changes", objectMapper.writeValueAsString(Map.of(
                        "summary", notes, "reason", "r",
                        "changes", List.of(Map.of("op", "UPDATE", "activityId", aActivity, "notes", notes, "reason", "r"))))))
                .thenText("Xong");
        String stream = chat(alice, aTrip, Map.of("message", notes));
        JsonNode proposal = eventData(stream, "proposal");
        assertThat(proposal).as(stream).isNotNull();
        return proposal.get("proposalId").asLong();
    }

    private PlaceEntity seedPlace(String name) {
        return placeRepository.save(PlaceEntity.builder()
                .provider("GOOGLE")
                .externalId("seed-" + UUID.randomUUID())
                .name(name)
                .category("restaurant")
                .latitude(new BigDecimal("16.06"))
                .longitude(new BigDecimal("108.24"))
                .adoptedVia(PlaceAdoption.SAVED)
                .build());
    }
}
