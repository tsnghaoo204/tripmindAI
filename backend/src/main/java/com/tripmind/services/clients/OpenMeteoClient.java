package com.tripmind.services.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.tripmind.configurations.properties.WeatherProperties;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** Gọi Open-Meteo (không cần khoá). Lỗi mạng hoặc phản hồi hỏng đều ném {@code EXTERNAL_SERVICE_ERROR}. */
@Slf4j
@Component
public class OpenMeteoClient {

    private static final String FORECAST_FIELDS =
            "temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum,weather_code";
    private static final String ARCHIVE_FIELDS =
            "temperature_2m_max,temperature_2m_min,precipitation_sum,weather_code";

    private final WebClient forecastClient;
    private final WebClient archiveClient;
    private final WeatherProperties properties;

    public OpenMeteoClient(WebClient.Builder builder, WeatherProperties properties) {
        this.properties = properties;
        this.forecastClient = builder.clone().baseUrl(properties.baseUrl()).build();
        this.archiveClient = builder.clone().baseUrl(properties.archiveBaseUrl()).build();
    }

    public List<RawDay> forecast(BigDecimal lat, BigDecimal lng, ZoneId zone, LocalDate from, LocalDate to) {
        return fetch(forecastClient, "/forecast", FORECAST_FIELDS, lat, lng, zone, from, to);
    }

    public List<RawDay> archive(BigDecimal lat, BigDecimal lng, ZoneId zone, LocalDate from, LocalDate to) {
        return fetch(archiveClient, "/archive", ARCHIVE_FIELDS, lat, lng, zone, from, to);
    }

    private List<RawDay> fetch(WebClient client, String path, String fields, BigDecimal lat, BigDecimal lng,
                               ZoneId zone, LocalDate from, LocalDate to) {
        JsonNode body;
        try {
            body = client.get()
                    .uri(uri -> uri.path(path)
                            .queryParam("latitude", lat.toPlainString())
                            .queryParam("longitude", lng.toPlainString())
                            .queryParam("daily", fields)
                            .queryParam("timezone", zone.getId())
                            .queryParam("start_date", from)
                            .queryParam("end_date", to)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(properties.timeout())
                    .retryWhen(Retry.backoff(1, Duration.ofMillis(300)))
                    .block();
        } catch (RuntimeException e) {
            log.warn("Open-Meteo {} {}..{} loi: {}", path, from, to, e.getMessage());
            throw new AppException(ErrorCode.EXTERNAL_SERVICE_ERROR, "Open-Meteo unavailable");
        }
        JsonNode daily = body == null ? null : body.get("daily");
        if (daily == null || !daily.has("time")) {
            throw new AppException(ErrorCode.EXTERNAL_SERVICE_ERROR, "Open-Meteo returned no daily data");
        }
        List<RawDay> days = new ArrayList<>();
        JsonNode time = daily.get("time");
        for (int i = 0; i < time.size(); i++) {
            days.add(new RawDay(
                    LocalDate.parse(time.get(i).asText()),
                    number(daily, "temperature_2m_min", i),
                    number(daily, "temperature_2m_max", i),
                    integer(daily, "precipitation_probability_max", i),
                    number(daily, "precipitation_sum", i),
                    integer(daily, "weather_code", i)));
        }
        return days;
    }

    private Double number(JsonNode daily, String field, int i) {
        JsonNode values = daily.get(field);
        return values == null || values.get(i) == null || values.get(i).isNull() ? null : values.get(i).asDouble();
    }

    private Integer integer(JsonNode daily, String field, int i) {
        Double value = number(daily, field, i);
        return value == null ? null : (int) Math.round(value);
    }

    /** Một ngày số liệu thô từ Open-Meteo; trường nào API không trả thì null. */
    public record RawDay(LocalDate date, Double tempMin, Double tempMax, Integer precipitationProbability,
                         Double precipitationMm, Integer weatherCode) {
    }
}
