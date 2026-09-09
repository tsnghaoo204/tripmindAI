package com.tripmind.services;

import com.tripmind.entities.PlaceEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DistanceServiceTest {

    private DistanceService distanceService;

    @BeforeEach
    void setUp() {
        distanceService = new DistanceService();
    }

    @Test
    @DisplayName("Should accurately calculate distance between Dragon Bridge and My Khe Beach")
    void shouldCalculateDistanceAccurately() {
        // Dragon Bridge Da Nang: 16.0610, 108.2274
        // My Khe Beach: 16.0592, 108.2435
        long distance = distanceService.calculateDistanceMeters(16.0610, 108.2274, 16.0592, 108.2435);

        // Geodesic distance is approx 1.7km (1700 - 1800m)
        assertThat(distance).isBetween(1600L, 1900L);
        assertThat(distanceService.formatDistance(distance)).isEqualTo("1.7 km");
    }

    @Test
    @DisplayName("Should estimate travel time by motorbike and walking")
    void shouldEstimateTravelTime() {
        PlaceEntity p1 = PlaceEntity.builder()
                .latitude(new BigDecimal("16.0610"))
                .longitude(new BigDecimal("108.2274"))
                .build();
        PlaceEntity p2 = PlaceEntity.builder()
                .latitude(new BigDecimal("16.0592"))
                .longitude(new BigDecimal("108.2435"))
                .build();

        DistanceService.TravelEstimate motoEstimate = distanceService.estimateTravel(p1, p2, "MOTORBIKE");
        assertThat(motoEstimate).isNotNull();
        assertThat(motoEstimate.getDistanceMeters()).isGreaterThan(0);
        assertThat(motoEstimate.getTravelTimeMinutes()).isGreaterThan(2);

        DistanceService.TravelEstimate walkEstimate = distanceService.estimateTravel(p1, p2, "WALKING");
        assertThat(walkEstimate).isNotNull();
        // Walking takes substantially longer than motorbike
        assertThat(walkEstimate.getTravelTimeMinutes()).isGreaterThan(motoEstimate.getTravelTimeMinutes());
    }

    @Test
    @DisplayName("Should recommend ideal timing based on spot characteristics")
    void shouldRecommendIdealTiming() {
        PlaceEntity beach = PlaceEntity.builder()
                .name("Bãi biển Mỹ Khê")
                .category("BEACH")
                .build();
        String beachTip = distanceService.getIdealTimingTip(beach, "Tắm biển & dạo bờ cát");
        assertThat(beachTip).contains("16:30 - 18:30");

        PlaceEntity nightMarket = PlaceEntity.builder()
                .name("Chợ đêm Sơn Trà")
                .category("SHOPPING")
                .build();
        String marketTip = distanceService.getIdealTimingTip(nightMarket, "Ăn vặt & mua quà lưu niệm");
        assertThat(marketTip).contains("18:30 - 21:30");

        PlaceEntity sunriseCape = PlaceEntity.builder()
                .name("Mũi Điện Đại Lãnh")
                .category("NATURAL_FEATURE")
                .build();
        String sunriseTip = distanceService.getIdealTimingTip(sunriseCape, "Đón bình minh đầu tiên");
        assertThat(sunriseTip).contains("05:00 - 06:30");
    }
}
