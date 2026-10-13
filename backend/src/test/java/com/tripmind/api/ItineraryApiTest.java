package com.tripmind.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tripmind.services.PlaceCandidateCache;
import com.tripmind.services.clients.GooglePlacesClient.GooglePlace;
import com.tripmind.support.IntegrationTest;
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

class ItineraryApiTest extends IntegrationTest {

    @Autowired
    private PlaceCandidateCache candidateCache;

    @Autowired
    private JdbcTemplate jdbc;

    private final LocalDate start = LocalDate.now().plusDays(5);

    @Test
    @DisplayName("Lịch trình của người khác trả 404; sắp lại với mã trùng trả 422 thay vì 500")
    void ownershipAndReorderValidation() throws Exception {
        String token = registerUser();
        long tripId = createTrip(token, start.toString(), start.plusDays(1).toString()).get("id").asLong();
        long a = addActivity(token, tripId, "Cau Rong").get("id").asLong();
        long b = addActivity(token, tripId, "Cho Han").get("id").asLong();
        long dayId = get("/api/trips/" + tripId + "/itinerary", token, 200).at("/data/days/0/id").asLong();

        get("/api/trips/" + tripId + "/itinerary", registerUser(), 404);

        JsonNode error = call(put("/api/itinerary-days/" + dayId + "/reorder"), token,
                Map.of("activityIds", List.of(a, a)), 422);
        assertThat(error.get("code").asText()).isEqualTo("REORDER_SET_MISMATCH");

        JsonNode reordered = call(put("/api/itinerary-days/" + dayId + "/reorder"), token,
                Map.of("activityIds", List.of(b, a)), 200).get("data");
        assertThat(reordered.at("/activities/0/id").asLong()).isEqualTo(b);
    }

    @Test
    @DisplayName("Thêm hoạt động bằng mã Google vừa tìm: địa điểm được nạp với adopted_via = ITINERARY")
    void addActivityAdoptsSearchResult() throws Exception {
        String token = registerUser();
        long tripId = createTrip(token, start.toString(), start.toString()).get("id").asLong();
        String externalId = "test-" + UUID.randomUUID();
        candidateCache.putAll("GOOGLE", List.of(new GooglePlace(externalId, "Quan Be Man", "Vo Nguyen Giap, Da Nang",
                new BigDecimal("16.061"), new BigDecimal("108.247"), "Vietnam", "VN", "Da Nang",
                List.of("restaurant"), new BigDecimal("4.4"), 3200, 2, null, null, null, null, null)));

        JsonNode activity = call(post("/api/trips/" + tripId + "/itinerary/activities"), token, Map.of(
                "dayNumber", 1, "title", "An hai san", "activityType", "FOOD",
                "placeExternalId", externalId, "estimatedCost", 400000, "estimatedCostSource", "PRICE_LEVEL"), 201).get("data");

        assertThat(activity.at("/place/name").asText()).isEqualTo("Quan Be Man");
        assertThat(activity.get("estimatedCostSource").asText()).isEqualTo("PRICE_LEVEL");
        String adoptedVia = jdbc.queryForObject(
                "SELECT adopted_via FROM places WHERE external_id = ?", String.class, externalId);
        assertThat(adoptedVia).isEqualTo("ITINERARY");

        JsonNode estimate = get("/api/trips/" + tripId + "/cost-estimate?placeId=" + activity.at("/place/id").asLong()
                + "&activityType=FOOD", token, 200).get("data");
        assertThat(estimate.get("suggested").asLong()).isEqualTo(400_000L);
        assertThat(estimate.get("basis").asText()).contains("$$");
    }

    @Test
    @DisplayName("Giờ kết thúc trước giờ bắt đầu bị từ chối; chi phí bỏ trống giữ là null, không thành 0")
    void timeRangeAndNullCost() throws Exception {
        String token = registerUser();
        long tripId = createTrip(token, start.toString(), start.toString()).get("id").asLong();

        JsonNode error = call(post("/api/trips/" + tripId + "/itinerary/activities"), token, Map.of(
                "dayNumber", 1, "title", "Bao tang", "activityType", "SIGHTSEEING",
                "startTime", "10:00", "endTime", "09:00"), 400);
        assertThat(error.get("code").asText()).isEqualTo("INVALID_TIME_RANGE");

        JsonNode activity = addActivity(token, tripId, "Di dao");
        assertThat(activity.get("estimatedCost").isNull() || activity.get("estimatedCost").isMissingNode()).isTrue();
    }

    @Test
    @DisplayName("Lấy và chọn phương án thay thế theo ngân sách (3 lựa chọn cho Ăn uống)")
    void getAndSelectActivityAlternatives() throws Exception {
        String token = registerUser();
        long tripId = createTrip(token, start.toString(), start.toString()).get("id").asLong();

        JsonNode activity = call(post("/api/trips/" + tripId + "/itinerary/activities"), token, Map.of(
                "dayNumber", 1, "title", "An trua quan com", "activityType", "FOOD",
                "estimatedCost", 150000), 201).get("data");
        long actId = activity.get("id").asLong();

        // 1. Lấy 3 lựa chọn
        JsonNode alts = get("/api/activities/" + actId + "/alternatives", token, 200).get("data");
        assertThat(alts).hasSize(3);
        assertThat(alts.get(0).get("tier").asText()).isEqualTo("BUDGET");
        assertThat(alts.get(1).get("tier").asText()).isEqualTo("BALANCED");
        assertThat(alts.get(2).get("tier").asText()).isEqualTo("PREMIUM");
        assertThat(alts.get(1).get("isSelected").asBoolean()).isTrue();

        // 2. Chọn phương án 1 (BUDGET)
        long budgetAltId = alts.get(0).get("id").asLong();
        JsonNode updated = call(put("/api/activities/" + actId + "/alternatives/" + budgetAltId + "/select"), token, null, 200).get("data");
        assertThat(updated.get("title").asText()).contains("Ẩm thực bình dân");
        assertThat(updated.get("estimatedCost").asLong()).isLessThanOrEqualTo(alts.get(1).get("estimatedCost").asLong());

        // 3. Người dùng khác không được truy cập
        get("/api/activities/" + actId + "/alternatives", registerUser(), 404);
        call(put("/api/activities/" + actId + "/alternatives/" + budgetAltId + "/select"), registerUser(), null, 404);
    }

    private JsonNode addActivity(String token, long tripId, String title) throws Exception {
        return call(post("/api/trips/" + tripId + "/itinerary/activities"), token,
                Map.of("dayNumber", 1, "title", title, "activityType", "SIGHTSEEING"), 201).get("data");
    }
}
