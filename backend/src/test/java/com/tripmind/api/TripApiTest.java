package com.tripmind.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tripmind.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

class TripApiTest extends IntegrationTest {

    private final LocalDate start = LocalDate.now().plusDays(10);

    @Test
    @DisplayName("Tạo chuyến lưu sở thích + nhóm đi; sửa chuyến dời ngày không vấp UNIQUE (trip_id, date)")
    void createAndShiftTrip() throws Exception {
        String token = registerUser();
        JsonNode trip = call(post("/api/trips"), token, Map.of(
                "destinationId", 1,
                "name", "Da Nang gia dinh",
                "startDate", start.toString(),
                "endDate", start.plusDays(3).toString(),
                "travelers", 4,
                "travelStyle", "RELAXED",
                "preferences", java.util.List.of("bien", "hai san", "bien"),
                "groupProfile", Map.of("children", 1, "dietary", java.util.List.of("VEGETARIAN"))), 201).get("data");

        long tripId = trip.get("id").asLong();
        assertThat(trip.get("travelStyle").asText()).isEqualTo("RELAXED");
        assertThat(trip.get("preferences")).hasSize(2);
        assertThat(trip.at("/groupProfile/children").asInt()).isEqualTo(1);
        assertThat(trip.get("days")).hasSize(4);
        assertThat(trip.get("phase").asText()).isEqualTo("BEFORE");

        // Sở thích của chuyến đầu tiên trở thành mặc định của tài khoản.
        JsonNode prefs = get("/api/me/preferences", token, 200).get("data");
        assertThat(prefs.get("travelStyle").asText()).isEqualTo("RELAXED");

        // Chỉ gửi ngày đi: dời cả chuyến 1 ngày, giữ 4 ngày. Ngày cũ của ngày 2 = ngày mới của ngày 1.
        JsonNode shifted = call(put("/api/trips/" + tripId), token,
                Map.of("startDate", start.plusDays(1).toString()), 200).get("data");
        assertThat(shifted.get("endDate").asText()).isEqualTo(start.plusDays(4).toString());
        assertThat(shifted.get("days")).hasSize(4);
        assertThat(shifted.at("/days/0/date").asText()).isEqualTo(start.plusDays(1).toString());
    }

    @Test
    @DisplayName("Chuyến dài hơn 30 ngày bị từ chối 422 TRIP_TOO_LONG")
    void tripTooLong() throws Exception {
        String token = registerUser();
        JsonNode error = call(post("/api/trips"), token, Map.of(
                "destinationId", 1, "name", "Qua dai",
                "startDate", start.toString(), "endDate", start.plusDays(30).toString()), 422);
        assertThat(error.get("code").asText()).isEqualTo("TRIP_TOO_LONG");

        call(post("/api/trips"), token, Map.of(
                "destinationId", 1, "name", "Vua du",
                "startDate", start.toString(), "endDate", start.plusDays(29).toString()), 201);
    }

    @Test
    @DisplayName("Rút ngắn chuyến khi ngày cuối còn hoạt động cần confirmDropDays")
    void shrinkTripNeedsConfirmation() throws Exception {
        String token = registerUser();
        JsonNode trip = createTrip(token, start.toString(), start.plusDays(3).toString());
        long tripId = trip.get("id").asLong();
        call(post("/api/trips/" + tripId + "/itinerary/activities"), token,
                Map.of("dayNumber", 4, "title", "Ra san bay", "activityType", "TRANSPORT"), 201);

        JsonNode error = call(put("/api/trips/" + tripId), token,
                Map.of("endDate", start.plusDays(2).toString()), 422);
        assertThat(error.get("code").asText()).isEqualTo("DAYS_HAVE_ACTIVITIES");
        assertThat(error.at("/details/dayNumbers/0").asInt()).isEqualTo(4);

        JsonNode shrunk = call(put("/api/trips/" + tripId), token,
                Map.of("endDate", start.plusDays(2).toString(), "confirmDropDays", true), 200).get("data");
        assertThat(shrunk.get("days")).hasSize(3);
    }

    @Test
    @DisplayName("Người khác gọi chuyến của mình nhận 404, không phải 403 (BR-104)")
    void otherUserGets404() throws Exception {
        String owner = registerUser();
        String stranger = registerUser();
        long tripId = createTrip(owner, start.toString(), start.plusDays(1).toString()).get("id").asLong();

        get("/api/trips/" + tripId, stranger, 404);
    }

    @Test
    @DisplayName("Đặt tay giai đoạn và lọc danh sách theo status")
    void phaseOverrideAndFilter() throws Exception {
        String token = registerUser();
        long tripId = createTrip(token, start.toString(), start.plusDays(1).toString()).get("id").asLong();

        JsonNode updated = call(put("/api/trips/" + tripId + "/phase"), token, Map.of("phase", "DURING"), 200).get("data");
        assertThat(updated.get("phase").asText()).isEqualTo("DURING");
        assertThat(updated.get("phaseSource").asText()).isEqualTo("MANUAL");

        assertThat(get("/api/trips?status=ongoing", token, 200).get("data")).hasSize(1);
        assertThat(get("/api/trips?status=upcoming", token, 200).get("data")).isEmpty();
    }
}
