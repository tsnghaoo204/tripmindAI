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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class GenerationApiTest extends IntegrationTest {

    @Autowired
    private PlaceRepository placeRepository;

    @BeforeEach
    void resetModel() {
        llm.reset();
    }

    @Test
    @DisplayName("Sinh lịch trình: địa điểm có thật được giữ, địa điểm bịa bị loại và đếm lại; chuyến đã có hoạt động thì 409")
    void generateItinerary() throws Exception {
        String token = registerUser();
        LocalDate start = LocalDate.now().plusDays(30);
        long tripId = createTrip(token, start.toString(), start.plusDays(1).toString()).get("id").asLong();
        String tag = UUID.randomUUID().toString().substring(0, 6);
        seedPlace("Ngu Hanh Son " + tag);
        seedPlace("Bao tang Dieu khac Cham " + tag);

        String plan = objectMapper.writeValueAsString(Map.of("days", List.of(
                Map.of("dayNumber", 1, "items", List.of(
                        Map.of("title", "Leo Ngũ Hành Sơn", "activityType", "SIGHTSEEING",
                                "placeName", "Ngũ Hành Sơn " + tag, "searchQuery", "Ngu Hanh Son " + tag, "startTime", "07:00"),
                        Map.of("title", "Nghỉ trưa", "activityType", "REST"),
                        Map.of("title", "Lâu đài tưởng tượng", "activityType", "SIGHTSEEING",
                                "placeName", "Lau dai Pha Le " + tag, "searchQuery", "Lau dai Pha Le " + tag))),
                Map.of("dayNumber", 2, "items", List.of(
                        Map.of("title", "Bảo tàng Chăm", "activityType", "SIGHTSEEING",
                                "placeName", "Bảo tàng Điêu khắc Chăm " + tag, "searchQuery", "Bao tang Dieu khac Cham " + tag))))));
        llm.thenText("```json\n" + plan + "\n```");

        JsonNode job = call(post("/api/trips/" + tripId + "/ai/generate"), token, null, 202).get("data");
        String jobId = job.get("jobId").asText();
        JsonNode status = waitForJob(token, tripId, jobId);

        assertThat(status.get("state").asText()).as(status.toString()).isEqualTo("DONE");
        assertThat(status.at("/result/activitiesCreated").asInt()).isEqualTo(3);
        assertThat(status.at("/result/rejectedCount").asInt()).isEqualTo(1);
        assertThat(status.at("/result/rejected/0/reason").asText()).isEqualTo("PLACE_NOT_FOUND");

        JsonNode itinerary = get("/api/trips/" + tripId + "/itinerary", token, 200).get("data");
        assertThat(itinerary.get("totalActivities").asInt()).isEqualTo(3);
        long generated = itinerary.at("/days/0/activities/0/id").asLong();
        assertThat(itinerary.at("/days/0/activities/0/createdBy").asText()).isEqualTo("AI");
        JsonNode explanation = get("/api/activities/" + generated + "/explanation", token, 200).get("data");
        assertThat(explanation.get("known").asBoolean()).isTrue();
        assertThat(explanation.get("tools")).hasSize(3);

        assertThat(call(post("/api/trips/" + tripId + "/ai/generate"), token, null, 409).get("code").asText())
                .isEqualTo("TRIP_NOT_EMPTY");
        get("/api/trips/" + tripId + "/ai/generate/" + jobId, registerUser(), 404);
    }

    private JsonNode waitForJob(String token, long tripId, String jobId) throws Exception {
        JsonNode status = null;
        for (int i = 0; i < 200; i++) {
            status = get("/api/trips/" + tripId + "/ai/generate/" + jobId, token, 200).get("data");
            if (!"RUNNING".equals(status.get("state").asText()) && !"QUEUED".equals(status.get("state").asText())) {
                return status;
            }
            Thread.sleep(50);
        }
        return status;
    }

    private void seedPlace(String name) {
        placeRepository.save(PlaceEntity.builder()
                .provider("GOOGLE")
                .externalId("seed-" + UUID.randomUUID())
                .name(name)
                .category("tourist_attraction")
                .latitude(new BigDecimal("16.0"))
                .longitude(new BigDecimal("108.2"))
                .adoptedVia(PlaceAdoption.SAVED)
                .build());
    }
}
