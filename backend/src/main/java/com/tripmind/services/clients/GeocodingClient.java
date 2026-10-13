package com.tripmind.services.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.tripmind.configurations.properties.WeatherProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Optional;

/** Tra toạ độ theo tên địa danh qua Open-Meteo Geocoding (không cần khoá). */
@Slf4j
@Component
public class GeocodingClient {

    private final WebClient client;
    private final WeatherProperties properties;

    public GeocodingClient(WebClient.Builder builder, WeatherProperties properties) {
        this.properties = properties;
        this.client = builder.clone().baseUrl(properties.geocodingBaseUrl()).build();
    }

    public Optional<GeoPoint> search(String name, String country) {
        JsonNode results;
        try {
            JsonNode body = client.get()
                    .uri(uri -> uri.path("/search")
                            .queryParam("name", name.trim())
                            .queryParam("count", 10)
                            .queryParam("language", "vi")
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(properties.timeout())
                    .block();
            results = body == null ? null : body.get("results");
        } catch (RuntimeException e) {
            log.warn("Geocoding '{}' loi: {}", name, e.getMessage());
            return Optional.empty();
        }
        if (results == null || results.isEmpty()) {
            return Optional.empty();
        }
        JsonNode best = results.get(0);
        for (JsonNode r : results) {
            if (country != null && normalize(country).equals(normalize(r.path("country").asText()))) {
                best = r;
                break;
            }
        }
        return Optional.of(new GeoPoint(new BigDecimal(best.get("latitude").asText()),
                new BigDecimal(best.get("longitude").asText()), best.path("timezone").asText(null)));
    }

    private static String normalize(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace("đ", "d").replace("Đ", "D").replace(" ", "").toLowerCase();
    }

    public record GeoPoint(BigDecimal latitude, BigDecimal longitude, String timezone) {
    }
}
