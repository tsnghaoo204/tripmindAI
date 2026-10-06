package com.tripmind.services;

import com.tripmind.configurations.properties.ChecklistProperties;
import com.tripmind.domains.models.GroupProfile;
import com.tripmind.domains.responses.ChecklistItemResponse;
import com.tripmind.domains.responses.ChecklistSuggestionsResponse;
import com.tripmind.domains.responses.TripWeatherResponse;
import com.tripmind.domains.responses.TripWeatherResponse.DayWeather;
import com.tripmind.entities.ActivityEntity;
import com.tripmind.entities.DestinationEntity;
import com.tripmind.entities.TripEntity;
import com.tripmind.enums.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

/**
 * Bộ luật gợi ý checklist, chạy bằng mã. Mỗi gợi ý có {@code reason} truy được về dữ liệu
 * (ngày nào mưa, hoạt động nào ở biển, nhóm có trẻ nhỏ…). Thời tiết lỗi thì bỏ qua nhóm luật
 * thời tiết và báo trong {@code skippedRules}, không đoán.
 */
@Component
@RequiredArgsConstructor
public class ChecklistSuggester {

    private static final List<String> BEACH_WORDS = List.of("biển", "bãi tắm", "beach", "bai bien");
    private static final List<String> FLIGHT_WORDS = List.of("bay", "sân bay", "flight", "airport");

    private final ChecklistProperties properties;

    public ChecklistSuggestionsResponse suggest(TripEntity trip, List<ActivityEntity> activities,
                                                TripWeatherResponse weather) {
        Map<String, ChecklistItemResponse> items = new LinkedHashMap<>();
        List<String> skipped = new ArrayList<>();

        add(items, ChecklistKind.PACK, "Giấy tờ tùy thân", ChecklistCategory.DOCUMENTS, null, "Luôn cần mang theo");
        add(items, ChecklistKind.PACK, "Sạc điện thoại", ChecklistCategory.ELECTRONICS, null, "Luôn cần mang theo");
        add(items, ChecklistKind.PACK, "Thuốc cá nhân", ChecklistCategory.HEALTH, null, "Luôn cần mang theo");

        if (weather == null || weather.getDays() == null || weather.getDays().isEmpty()) {
            skipped.add("WEATHER");
        } else {
            weatherRules(items, weather.getDays());
        }

        for (ActivityEntity a : activities) {
            String text = (a.getTitle() + " " + (a.getPlace() == null ? "" : a.getPlace().getName() + " "
                    + Objects.toString(a.getPlace().getCategory(), ""))).toLowerCase(Locale.ROOT);
            int day = a.getItineraryDay().getDayNumber();
            if (BEACH_WORDS.stream().anyMatch(text::contains)) {
                add(items, ChecklistKind.PACK, "Đồ bơi", ChecklistCategory.CLOTHING, null,
                        "Ngày " + day + " có " + label(a));
            }
            if (a.getActivityType() == ActivityType.TRANSPORT && FLIGHT_WORDS.stream().anyMatch(text::contains)) {
                add(items, ChecklistKind.TODO, "Check-in online chuyến bay", ChecklistCategory.BOOKING,
                        a.getItineraryDay().getDate().minusDays(1), "Ngày " + day + ": " + a.getTitle());
            }
            if (a.getActivityType() == ActivityType.ACCOMMODATION) {
                add(items, ChecklistKind.TODO, "Xác nhận lại đặt phòng", ChecklistCategory.BOOKING,
                        trip.getStartDate().minusDays(2), "Ngày " + day + ": " + a.getTitle());
            }
        }

        if (trip.lengthInDays() >= properties.longTripDays()) {
            String reason = "Chuyến dài " + trip.lengthInDays() + " ngày";
            add(items, ChecklistKind.PACK, "Sạc dự phòng", ChecklistCategory.ELECTRONICS, null, reason);
            add(items, ChecklistKind.PACK, "Túi đựng đồ giặt", ChecklistCategory.OTHER, null, reason);
        }

        if (isAbroad(trip.getDestination())) {
            String reason = "Điểm đến ở " + trip.getDestination().getCountry();
            LocalDate start = trip.getStartDate();
            add(items, ChecklistKind.PACK, "Hộ chiếu", ChecklistCategory.DOCUMENTS, null, reason);
            add(items, ChecklistKind.PACK, "Ổ cắm chuyển đổi", ChecklistCategory.ELECTRONICS, null, reason);
            add(items, ChecklistKind.TODO, "Đổi ngoại tệ", ChecklistCategory.MONEY, start.minusDays(3), reason);
            add(items, ChecklistKind.TODO, "Mua SIM hoặc eSIM du lịch", ChecklistCategory.OTHER, start.minusDays(1), reason);
        }

        GroupProfile group = trip.getGroupProfile();
        if (group != null && group.childrenCount() > 0) {
            String reason = "Nhóm có " + group.childrenCount() + " trẻ nhỏ";
            add(items, ChecklistKind.PACK, "Đồ dùng cho trẻ (bỉm, khăn ướt, đồ chơi)", ChecklistCategory.KIDS, null, reason);
            add(items, ChecklistKind.PACK, "Thuốc hạ sốt cho trẻ", ChecklistCategory.HEALTH, null, reason);
        }
        if (group != null && group.seniorsCount() > 0) {
            add(items, ChecklistKind.PACK, "Thuốc dùng hằng ngày của người lớn tuổi", ChecklistCategory.HEALTH, null,
                    "Nhóm có " + group.seniorsCount() + " người lớn tuổi");
        }

        return ChecklistSuggestionsResponse.builder()
                .items(new ArrayList<>(items.values()))
                .skippedRules(skipped.isEmpty() ? null : skipped)
                .build();
    }

    private void weatherRules(Map<String, ChecklistItemResponse> items, List<DayWeather> days) {
        for (DayWeather d : days) {
            boolean rainy = (d.getPrecipitationProbability() != null && d.getPrecipitationProbability() >= properties.rainProbability())
                    || (d.getSource() == WeatherSource.CLIMATE_NORMAL && d.getPrecipitationMm() != null
                    && d.getPrecipitationMm() >= properties.rainMm());
            if (rainy) {
                add(items, ChecklistKind.PACK, "Áo mưa hoặc ô", ChecklistCategory.CLOTHING, null,
                        "Ngày " + d.getDayNumber() + " " + sourceLabel(d.getSource()) + " mưa "
                                + Objects.toString(d.getPrecipitationProbability(), "?") + "%");
            }
            if (d.getTempMax() != null && d.getTempMax() >= properties.hotTempMax()) {
                String reason = "Ngày " + d.getDayNumber() + " " + sourceLabel(d.getSource()) + " nóng tới "
                        + Math.round(d.getTempMax()) + "°C";
                add(items, ChecklistKind.PACK, "Kem chống nắng", ChecklistCategory.TOILETRIES, null, reason);
                add(items, ChecklistKind.PACK, "Mũ và kính râm", ChecklistCategory.CLOTHING, null, reason);
            }
            if (d.getTempMin() != null && d.getTempMin() <= properties.coldTempMin()) {
                add(items, ChecklistKind.PACK, "Áo khoác ấm", ChecklistCategory.CLOTHING, null,
                        "Ngày " + d.getDayNumber() + " " + sourceLabel(d.getSource()) + " lạnh còn "
                                + Math.round(d.getTempMin()) + "°C");
            }
        }
    }

    private boolean isAbroad(DestinationEntity destination) {
        if (destination == null) {
            return false;
        }
        String code = destination.getMetadata() != null && destination.getMetadata().hasNonNull("countryCode")
                ? destination.getMetadata().get("countryCode").asText() : null;
        return properties.homeCountries().stream().noneMatch(home ->
                home.equalsIgnoreCase(destination.getCountry()) || (code != null && home.equalsIgnoreCase(code)));
    }

    private String sourceLabel(WeatherSource source) {
        return switch (source) {
            case FORECAST -> "dự báo";
            case CLIMATE_NORMAL -> "theo trung bình khí hậu";
            case OBSERVED -> "đã ghi nhận";
        };
    }

    private String label(ActivityEntity a) {
        return a.getPlace() != null ? a.getPlace().getName() : a.getTitle();
    }

    /** Giữ lý do của lần gặp đầu tiên; một mục chỉ xuất hiện một lần. */
    private void add(Map<String, ChecklistItemResponse> items, ChecklistKind kind, String title,
                     ChecklistCategory category, LocalDate dueDate, String reason) {
        items.putIfAbsent(kind + ":" + title.toLowerCase(Locale.ROOT), ChecklistItemResponse.builder()
                .kind(kind)
                .title(title)
                .category(category)
                .dueDate(dueDate)
                .source(ChecklistSource.SUGGESTED)
                .reason(reason)
                .build());
    }
}
