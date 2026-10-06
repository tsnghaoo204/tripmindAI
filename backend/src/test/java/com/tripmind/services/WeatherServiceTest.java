package com.tripmind.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripmind.configurations.properties.WeatherProperties;
import com.tripmind.domains.responses.TripWeatherResponse;
import com.tripmind.entities.DestinationEntity;
import com.tripmind.entities.TripEntity;
import com.tripmind.enums.WeatherSource;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import com.tripmind.services.clients.OpenMeteoClient;
import com.tripmind.services.clients.OpenMeteoClient.RawDay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WeatherServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 6);

    private final OpenMeteoClient client = mock(OpenMeteoClient.class);
    private WeatherService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(TODAY.atTime(5, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        WeatherProperties props = new WeatherProperties("http://f", "http://a", 16, 90, 3,
                Duration.ofSeconds(5), Duration.ofHours(3));
        service = new WeatherService(null, client, props, new TripClock(clock), mock(StringRedisTemplate.class), new ObjectMapper());

        when(client.forecast(any(), any(), any(), any(), any())).thenAnswer(inv ->
                days(inv.getArgument(3), inv.getArgument(4), 80, 12.0));
        when(client.archive(any(), any(), any(), any(), any())).thenAnswer(inv ->
                days(inv.getArgument(3), inv.getArgument(4), null, 0.5));
    }

    private static List<RawDay> days(LocalDate from, LocalDate to, Integer prob, double mm) {
        List<RawDay> result = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            result.add(new RawDay(d, 24.0, 31.0, prob, mm, 61));
        }
        return result;
    }

    private TripEntity trip(LocalDate start, LocalDate end) {
        return TripEntity.builder().id(1L).startDate(start).endDate(end)
                .destination(DestinationEntity.builder().latitude(new BigDecimal("16.05"))
                        .longitude(new BigDecimal("108.2")).timezone("Asia/Ho_Chi_Minh").build())
                .build();
    }

    @Test
    @DisplayName("Ngày thứ 16 tính từ hôm nay còn là dự báo, ngày thứ 17 là trung bình khí hậu")
    void forecastClimateBoundary() {
        TripWeatherResponse r = service.forTrip(trip(TODAY.plusDays(14), TODAY.plusDays(17)));

        assertThat(r.getDays()).extracting(TripWeatherResponse.DayWeather::getSource).containsExactly(
                WeatherSource.FORECAST, WeatherSource.FORECAST, WeatherSource.CLIMATE_NORMAL, WeatherSource.CLIMATE_NORMAL);
        assertThat(r.getDays().get(0).getDayNumber()).isEqualTo(1);
        assertThat(r.getDays().get(3).getPrecipitationProbability()).isZero(); // 0.5 mm < 1 mm ở cả 3 năm
        assertThat(r.getReason()).isNull();
        verify(client, times(3)).archive(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("API dự báo lỗi: vẫn 200 với phần khí hậu, reason = PARTIAL")
    void forecastDown() {
        doThrow(new AppException(ErrorCode.EXTERNAL_SERVICE_ERROR, "down"))
                .when(client).forecast(any(), any(), any(), any(), any());

        TripWeatherResponse r = service.forTrip(trip(TODAY.plusDays(14), TODAY.plusDays(17)));

        assertThat(r.getDays()).hasSize(2);
        assertThat(r.getReason()).isEqualTo(WeatherService.PARTIAL);
    }

    @Test
    @DisplayName("Open-Meteo chết hẳn: days rỗng kèm WEATHER_UNAVAILABLE, không ném lỗi")
    void allDown() {
        doThrow(new AppException(ErrorCode.EXTERNAL_SERVICE_ERROR, "down"))
                .when(client).forecast(any(), any(), any(), any(), any());
        doThrow(new AppException(ErrorCode.EXTERNAL_SERVICE_ERROR, "down"))
                .when(client).archive(any(), any(), any(), any(), any());

        TripWeatherResponse r = service.forTrip(trip(TODAY.plusDays(1), TODAY.plusDays(2)));

        assertThat(r.getDays()).isEmpty();
        assertThat(r.getReason()).isEqualTo(WeatherService.WEATHER_UNAVAILABLE);
    }

    @Test
    @DisplayName("Ngày đã qua trong cửa sổ dự báo được gắn nhãn OBSERVED")
    void pastDaysAreObserved() {
        TripWeatherResponse r = service.forTrip(trip(TODAY.minusDays(1), TODAY));
        assertThat(r.getDays()).extracting(TripWeatherResponse.DayWeather::getSource)
                .containsExactly(WeatherSource.OBSERVED, WeatherSource.FORECAST);
    }
}
