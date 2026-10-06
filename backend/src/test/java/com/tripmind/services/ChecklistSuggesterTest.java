package com.tripmind.services;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.tripmind.configurations.properties.ChecklistProperties;
import com.tripmind.domains.models.GroupProfile;
import com.tripmind.domains.responses.ChecklistItemResponse;
import com.tripmind.domains.responses.ChecklistSuggestionsResponse;
import com.tripmind.domains.responses.TripWeatherResponse;
import com.tripmind.entities.*;
import com.tripmind.enums.ActivityType;
import com.tripmind.enums.ChecklistKind;
import com.tripmind.enums.WeatherSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ChecklistSuggesterTest {

    private final ChecklistSuggester suggester = new ChecklistSuggester(
            new ChecklistProperties(50, 5, 32, 15, 4, List.of("VN", "Vietnam")));
    private final LocalDate start = LocalDate.of(2026, 10, 20);

    private TripEntity trip(String country, String countryCode, GroupProfile group, int days) {
        return TripEntity.builder().id(1L).startDate(start).endDate(start.plusDays(days - 1)).travelers((short) 3)
                .groupProfile(group)
                .destination(DestinationEntity.builder().name("X").country(country)
                        .metadata(countryCode == null ? null : JsonNodeFactory.instance.objectNode().put("countryCode", countryCode))
                        .build())
                .build();
    }

    private ActivityEntity activity(int day, String title, ActivityType type, String placeName) {
        ItineraryDayEntity d = ItineraryDayEntity.builder().dayNumber((short) day).date(start.plusDays(day - 1)).build();
        return ActivityEntity.builder().itineraryDay(d).title(title).activityType(type)
                .place(placeName == null ? null : PlaceEntity.builder().name(placeName).latitude(BigDecimal.ONE)
                        .longitude(BigDecimal.ONE).build())
                .build();
    }

    private Map<String, ChecklistItemResponse> byTitle(ChecklistSuggestionsResponse r) {
        return r.getItems().stream().collect(Collectors.toMap(ChecklistItemResponse::getTitle, i -> i));
    }

    @Test
    @DisplayName("Ngày dự báo mưa và hoạt động ở biển cho ra áo mưa và đồ bơi, kèm lý do truy được")
    void weatherAndBeach() {
        TripWeatherResponse weather = TripWeatherResponse.builder().days(List.of(
                TripWeatherResponse.DayWeather.builder().dayNumber(1).source(WeatherSource.FORECAST)
                        .precipitationProbability(10).tempMax(30.0).tempMin(24.0).build(),
                TripWeatherResponse.DayWeather.builder().dayNumber(2).source(WeatherSource.FORECAST)
                        .precipitationProbability(70).tempMax(34.4).tempMin(25.0).build())).build();

        Map<String, ChecklistItemResponse> items = byTitle(suggester.suggest(trip("Vietnam", "VN", GroupProfile.empty(), 2),
                List.of(activity(1, "Tắm biển", ActivityType.SIGHTSEEING, "Bãi biển Mỹ Khê")), weather));

        assertThat(items.get("Áo mưa hoặc ô").getReason()).isEqualTo("Ngày 2 dự báo mưa 70%");
        assertThat(items.get("Đồ bơi").getReason()).isEqualTo("Ngày 1 có Bãi biển Mỹ Khê");
        assertThat(items.get("Kem chống nắng").getReason()).isEqualTo("Ngày 2 dự báo nóng tới 34°C");
        assertThat(items).doesNotContainKey("Hộ chiếu");
    }

    @Test
    @DisplayName("Đi nước ngoài, có trẻ nhỏ, có chuyến bay: hộ chiếu, đồ cho trẻ, check-in online trước 1 ngày")
    void abroadWithKidsAndFlight() {
        ChecklistSuggestionsResponse r = suggester.suggest(trip("Japan", "JP", new GroupProfile(1, 0, null, null, null), 5),
                List.of(activity(1, "Bay tới Tokyo", ActivityType.TRANSPORT, null)), null);
        Map<String, ChecklistItemResponse> items = byTitle(r);

        assertThat(items).containsKeys("Hộ chiếu", "Đổi ngoại tệ", "Thuốc hạ sốt cho trẻ", "Sạc dự phòng");
        assertThat(items.get("Check-in online chuyến bay").getKind()).isEqualTo(ChecklistKind.TODO);
        assertThat(items.get("Check-in online chuyến bay").getDueDate()).isEqualTo(start.minusDays(1));
        assertThat(r.getSkippedRules()).containsExactly("WEATHER");
    }
}
