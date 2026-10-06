package com.tripmind.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tripmind.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

class BudgetApiTest extends IntegrationTest {

    @Test
    @DisplayName("Chi tiêu CRUD, ước tính bỏ hoạt động SKIPPED, khác tiền bị từ chối")
    void expensesAndBudget() throws Exception {
        String token = registerUser();
        LocalDate start = LocalDate.now().plusDays(3);
        long tripId = createTrip(token, start.toString(), start.plusDays(1).toString()).get("id").asLong();

        long dinner = call(post("/api/trips/" + tripId + "/itinerary/activities"), token, Map.of(
                "dayNumber", 1, "title", "An toi", "activityType", "FOOD", "estimatedCost", 600000), 201)
                .at("/data/id").asLong();
        long tour = call(post("/api/trips/" + tripId + "/itinerary/activities"), token, Map.of(
                "dayNumber", 1, "title", "Tour", "activityType", "SIGHTSEEING", "estimatedCost", 1000000), 201)
                .at("/data/id").asLong();
        call(put("/api/activities/" + tour), token, Map.of("status", "SKIPPED", "skipReason", "RAIN"), 200);

        JsonNode expense = call(post("/api/trips/" + tripId + "/expenses"), token, Map.of(
                "category", "ACCOMMODATION", "amount", 2500000, "expenseDate", LocalDate.now().toString(),
                "description", "Dat phong truoc"), 201).get("data");
        call(post("/api/trips/" + tripId + "/expenses"), token, Map.of(
                "category", "FOOD", "amount", 450000, "expenseDate", start.toString(), "activityId", dinner), 201);

        JsonNode mismatch = call(post("/api/trips/" + tripId + "/expenses"), token, Map.of(
                "category", "FOOD", "amount", 20, "currency", "USD", "expenseDate", start.toString()), 422);
        assertThat(mismatch.get("code").asText()).isEqualTo("CURRENCY_MISMATCH");

        JsonNode budget = get("/api/trips/" + tripId + "/budget", token, 200).get("data");
        assertThat(budget.get("estimatedTotal").asLong()).isEqualTo(600_000L);
        assertThat(budget.get("actualTotal").asLong()).isEqualTo(2_950_000L);
        assertThat(budget.get("remaining").asLong()).isEqualTo(8_000_000L - 2_950_000L);
        assertThat(budget.at("/byCategory/FOOD/actual").asLong()).isEqualTo(450_000L);
        assertThat(budget.at("/daily/mode").asText()).isEqualTo("PLAN");

        assertThat(get("/api/trips/" + tripId + "/expenses?category=FOOD", token, 200).get("data")).hasSize(1);
        call(delete("/api/expenses/" + expense.get("id").asLong()), token, null, 200);
        get("/api/trips/" + tripId + "/expenses", registerUser(), 404);

        JsonNode currencyChange = call(put("/api/trips/" + tripId), token, Map.of("currency", "USD"), 422);
        assertThat(currencyChange.get("code").asText()).isEqualTo("CURRENCY_MISMATCH");
    }
}
