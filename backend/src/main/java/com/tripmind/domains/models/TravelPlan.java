package com.tripmind.domains.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Hành trình của chuyến, lưu ở cột {@code trips.travel_plan}; mọi trường đều không bắt buộc. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TravelPlan(
        @Size(max = 120) String origin,
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String arrivalTime,
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String departureTime,
        @Pattern(regexp = "^(AUTO|BUS|TRAIN|FLIGHT|CAR|MOTORBIKE)$") String transportMode,
        @Pattern(regexp = "^(TAXI|MOTORBIKE_RENTAL|OWN_VEHICLE|WALK)$") String localTransport,
        @Pattern(regexp = "^(HOMESTAY|HOTEL|RESORT|BOOKED)$") String lodgingType,
        @Size(max = 160) String lodgingName,
        Boolean budgetIncludesTransport,
        @Size(max = 200) String avoid) {

    public static TravelPlan empty() {
        return new TravelPlan(null, null, null, null, null, null, null, null, null);
    }

    @JsonIgnore
    public boolean hasOrigin() {
        return origin != null && !origin.isBlank();
    }
}
