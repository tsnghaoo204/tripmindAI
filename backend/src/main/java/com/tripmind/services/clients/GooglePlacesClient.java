package com.tripmind.services.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Wrapper quanh Google Places API (New) v1 và Time Zone API.
 *
 * <p>Ba thao tác dùng cho việc nạp điểm đến/địa điểm trên toàn cầu:
 * <ul>
 *   <li>{@link #searchText(String, String)} — Text Search, trả về danh sách ứng viên;</li>
 *   <li>{@link #findPlace(String)} — lấy đúng một kết quả khớp nhất cho một chuỗi tìm kiếm;</li>
 *   <li>{@link #getPlaceDetails(String)} — Place Details theo {@code place_id}.</li>
 * </ul>
 *
 * <p>Places API không trả về múi giờ IANA, nên {@link #resolveTimezone(BigDecimal, BigDecimal)}
 * hỏi Time Zone API riêng — {@code destinations.timezone} là {@code NOT NULL} nên bước
 * này bắt buộc trước khi ghi một điểm đến mới.
 *
 * <p>Khi {@code tripmind.google.places.use-mock = true}, mọi lượt gọi ra ngoài bị chặn và
 * client trả về rỗng: hệ thống chạy tiếp bằng catalog đã có trong cơ sở dữ liệu.
 */
@Slf4j
@Component
public class GooglePlacesClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    /** Trường cần cho một ứng viên trong danh sách kết quả. */
    private static final String SEARCH_FIELD_MASK = String.join(",",
            "places.id",
            "places.displayName",
            "places.formattedAddress",
            "places.location",
            "places.types",
            "places.rating",
            "places.userRatingCount",
            "places.priceLevel",
            "places.addressComponents");

    /** Trường cần khi xem chi tiết một địa điểm. */
    private static final String DETAILS_FIELD_MASK = String.join(",",
            "id",
            "displayName",
            "formattedAddress",
            "location",
            "types",
            "rating",
            "userRatingCount",
            "priceLevel",
            "addressComponents",
            "internationalPhoneNumber",
            "websiteUri",
            "regularOpeningHours",
            "photos");

    private final WebClient.Builder webClientBuilder;

    @Value("${tripmind.google.places.api-key}")
    private String apiKey;

    @Value("${tripmind.google.places.base-url}")
    private String baseUrl;

    @Value("${tripmind.google.places.timezone-url}")
    private String timezoneUrl;

    @Value("${tripmind.google.places.language}")
    private String language;

    @Value("${tripmind.google.places.max-results}")
    private int maxResults;

    @Value("${tripmind.google.places.use-mock}")
    private boolean useMock;

    private WebClient placesClient;
    private WebClient timezoneClient;

    public GooglePlacesClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @PostConstruct
    void init() {
        this.placesClient = webClientBuilder.baseUrl(baseUrl).build();
        this.timezoneClient = webClientBuilder.baseUrl(timezoneUrl).build();
        if (useMock) {
            log.warn("Google Places dang o che do mock: khong goi API that, chi dung catalog trong CSDL");
        }
    }

    public boolean isEnabled() {
        return !useMock && apiKey != null && !apiKey.isBlank();
    }

    /**
     * Text Search. {@code includedType} lọc theo một loại của Bảng A (ví dụ
     * {@code "restaurant"}); truyền {@code null} khi tìm thành phố, vì các loại địa chỉ
     * như {@code locality} không dùng được cho bộ lọc này.
     */
    public List<GooglePlace> searchText(String textQuery, String includedType) {
        if (textQuery == null || textQuery.isBlank()) {
            return List.of();
        }
        if (!isEnabled()) {
            log.debug("Bo qua Google searchText('{}'): client dang tat", textQuery);
            return List.of();
        }

        Map<String, Object> body = new HashMap<>();
        body.put("textQuery", textQuery);
        body.put("languageCode", language);
        body.put("maxResultCount", Math.clamp(maxResults, 1, 20));
        if (includedType != null && !includedType.isBlank()) {
            body.put("includedType", includedType);
        }

        JsonNode response = call(() -> placesClient.post()
                .uri("/places:searchText")
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", SEARCH_FIELD_MASK)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(TIMEOUT), "searchText:" + textQuery);

        JsonNode places = response == null ? null : response.get("places");
        if (places == null || !places.isArray()) {
            return List.of();
        }
        List<GooglePlace> results = new ArrayList<>(places.size());
        for (JsonNode place : places) {
            results.add(toGooglePlace(place));
        }
        return results;
    }

    /**
     * Kết quả khớp nhất cho một chuỗi tìm kiếm — dùng khi người dùng gõ thẳng tên điểm
     * đến thay vì chọn từ danh sách gợi ý.
     */
    public Optional<GooglePlace> findPlace(String textQuery) {
        return searchText(textQuery, null).stream().findFirst();
    }

    public Optional<GooglePlace> getPlaceDetails(String placeId) {
        if (placeId == null || placeId.isBlank()) {
            return Optional.empty();
        }
        if (!isEnabled()) {
            log.debug("Bo qua Google getPlaceDetails('{}'): client dang tat", placeId);
            return Optional.empty();
        }

        JsonNode response = call(() -> placesClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/places/{placeId}")
                        .queryParam("languageCode", language)
                        .build(placeId))
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", DETAILS_FIELD_MASK)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(TIMEOUT), "placeDetails:" + placeId);

        return response == null || response.get("id") == null
                ? Optional.empty()
                : Optional.of(toGooglePlace(response));
    }

    /**
     * Múi giờ IANA tại một toạ độ, ví dụ {@code Asia/Ho_Chi_Minh}. Rỗng khi Time Zone API
     * tắt trên khoá hoặc toạ độ nằm ngoài vùng phủ (giữa đại dương).
     */
    public Optional<String> resolveTimezone(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null || !isEnabled()) {
            return Optional.empty();
        }

        JsonNode response = call(() -> timezoneClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("location", latitude.toPlainString() + "," + longitude.toPlainString())
                        .queryParam("timestamp", Instant.now().getEpochSecond())
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(TIMEOUT), "timezone:" + latitude + "," + longitude);

        if (response == null || !"OK".equals(text(response, "status"))) {
            log.warn("Time Zone API khong tra ve mui gio cho ({}, {}): status={}",
                    latitude, longitude, response == null ? "null" : text(response, "status"));
            return Optional.empty();
        }
        return Optional.ofNullable(text(response, "timeZoneId"));
    }

    private JsonNode call(Supplier<JsonNode> request, String operation) {
        try {
            return request.get();
        } catch (WebClientResponseException e) {
            log.error("Google API loi o {}: {} {}", operation, e.getStatusCode(), e.getResponseBodyAsString());
            throw new AppException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "Google Places API tra ve loi " + e.getStatusCode().value());
        } catch (RuntimeException e) {
            log.error("Google API khong goi duoc o {}: {}", operation, e.getMessage());
            throw new AppException(ErrorCode.EXTERNAL_SERVICE_ERROR, "Khong ket noi duoc Google Places API");
        }
    }

    private GooglePlace toGooglePlace(JsonNode node) {
        JsonNode location = node.get("location");
        JsonNode displayName = node.get("displayName");

        List<String> types = new ArrayList<>();
        JsonNode typesNode = node.get("types");
        if (typesNode != null && typesNode.isArray()) {
            typesNode.forEach(type -> types.add(type.asText()));
        }

        return new GooglePlace(
                text(node, "id"),
                displayName == null ? text(node, "formattedAddress") : text(displayName, "text"),
                text(node, "formattedAddress"),
                decimal(location, "latitude"),
                decimal(location, "longitude"),
                addressComponent(node, "country", false),
                addressComponent(node, "country", true),
                addressComponent(node, "administrative_area_level_1", false),
                List.copyOf(types),
                node.hasNonNull("rating") ? node.get("rating").decimalValue() : null,
                node.hasNonNull("userRatingCount") ? node.get("userRatingCount").asInt() : null,
                priceLevel(text(node, "priceLevel")),
                text(node, "websiteUri"),
                text(node, "internationalPhoneNumber"),
                node.get("regularOpeningHours"),
                node.get("photos"),
                node);
    }

    /**
     * Tên nước / tỉnh lấy từ {@code addressComponents}. {@code shortName = true} cho mã
     * hai ký tự (VN, JP) dùng làm {@code country_code}.
     */
    private String addressComponent(JsonNode node, String type, boolean shortName) {
        JsonNode components = node.get("addressComponents");
        if (components == null || !components.isArray()) {
            return null;
        }
        for (JsonNode component : components) {
            JsonNode types = component.get("types");
            if (types == null || !types.isArray()) {
                continue;
            }
            for (JsonNode t : types) {
                if (type.equals(t.asText())) {
                    return text(component, shortName ? "shortText" : "longText");
                }
            }
        }
        return null;
    }

    /** {@code PRICE_LEVEL_MODERATE} → {@code 2}, theo thang 0-4 của bảng {@code places}. */
    private Integer priceLevel(String priceLevel) {
        if (priceLevel == null) {
            return null;
        }
        return switch (priceLevel) {
            case "PRICE_LEVEL_FREE" -> 0;
            case "PRICE_LEVEL_INEXPENSIVE" -> 1;
            case "PRICE_LEVEL_MODERATE" -> 2;
            case "PRICE_LEVEL_EXPENSIVE" -> 3;
            case "PRICE_LEVEL_VERY_EXPENSIVE" -> 4;
            default -> null;
        };
    }

    private String text(JsonNode node, String field) {
        return node != null && node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    private BigDecimal decimal(JsonNode node, String field) {
        return node != null && node.hasNonNull(field) ? node.get(field).decimalValue() : null;
    }

    /**
     * Một địa điểm do Google trả về, đã bóc khỏi hình dạng JSON của nhà cung cấp.
     * {@code raw} giữ nguyên nút gốc để lưu vào cột {@code metadata}.
     */
    public record GooglePlace(
            String placeId,
            String name,
            String formattedAddress,
            BigDecimal latitude,
            BigDecimal longitude,
            String country,
            String countryCode,
            String adminArea,
            List<String> types,
            BigDecimal rating,
            Integer userRatingCount,
            Integer priceLevel,
            String websiteUri,
            String phoneNumber,
            JsonNode openingHours,
            JsonNode photos,
            JsonNode raw) {

        /** Đủ dữ liệu để ghi thành một dòng {@code destinations} hay chưa. */
        public boolean isResolvable() {
            return placeId != null && !placeId.isBlank()
                    && name != null && !name.isBlank()
                    && latitude != null && longitude != null;
        }

        /** Kết quả có phải một vùng địa lý (thành phố, tỉnh, đảo) chứ không phải một quán ăn. */
        public boolean isLocality() {
            return types.contains("locality")
                    || types.contains("administrative_area_level_1")
                    || types.contains("administrative_area_level_2")
                    || types.contains("political");
        }
    }
}
