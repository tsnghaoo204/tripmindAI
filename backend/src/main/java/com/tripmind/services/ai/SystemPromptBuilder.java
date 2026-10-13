package com.tripmind.services.ai;

import com.tripmind.domains.models.GroupProfile;
import com.tripmind.domains.models.TravelPlan;
import com.tripmind.entities.TripEntity;
import com.tripmind.enums.DietaryRestriction;
import com.tripmind.enums.TripPhase;
import com.tripmind.services.TripClock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lời nhắc hệ thống, dựng lại mỗi lượt hỏi từ mẫu {@code prompts/assistant-system.md}: tóm tắt
 * chuyến, ngày hôm nay theo múi giờ điểm đến, thông tin nhóm đi và các luật bắt buộc.
 */
@Component
public class SystemPromptBuilder {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Map<TripPhase, String> PHASES = Map.of(
            TripPhase.BEFORE, "chuyến đi chưa bắt đầu",
            TripPhase.DURING, "đang trong chuyến đi",
            TripPhase.AFTER, "chuyến đi đã kết thúc");
    private static final Map<String, String> TRANSPORT = Map.of(
            "AUTO", "chưa chọn, hãy đề xuất phương tiện hợp lý nhất", "BUS", "xe khách", "TRAIN", "tàu hỏa",
            "FLIGHT", "máy bay", "CAR", "ô tô riêng", "MOTORBIKE", "xe máy");
    private static final Map<String, String> LOCAL = Map.of(
            "TAXI", "taxi/Grab", "MOTORBIKE_RENTAL", "thuê xe máy", "OWN_VEHICLE", "xe riêng", "WALK", "đi bộ");
    private static final Map<String, String> LODGING = Map.of(
            "HOMESTAY", "homestay", "HOTEL", "khách sạn", "RESORT", "resort", "BOOKED", "đã đặt sẵn");
    private static final Map<DietaryRestriction, String> DIETS = Map.of(
            DietaryRestriction.VEGETARIAN, "ăn chay",
            DietaryRestriction.VEGAN, "ăn thuần chay",
            DietaryRestriction.HALAL, "ăn halal",
            DietaryRestriction.NO_SEAFOOD, "không ăn hải sản",
            DietaryRestriction.NO_PORK, "không ăn thịt lợn");

    private final String template;
    private final TripClock tripClock;

    public SystemPromptBuilder(@Value("classpath:prompts/assistant-system.md") Resource resource, TripClock tripClock) {
        this.tripClock = tripClock;
        try {
            this.template = resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Khong doc duoc mau loi nhac he thong", e);
        }
    }

    public String build(TripEntity trip) {
        return template
                .replace("{tripSummary}", tripSummary(trip))
                .replace("{today}", tripClock.today(trip).format(DATE))
                .replace("{phase}", PHASES.get(tripClock.phase(trip)))
                .replace("{groupSummary}", groupSummary(trip))
                .replace("{currency}", trip.getCurrency());
    }

    public String tripSummary(TripEntity trip) {
        List<String> parts = new ArrayList<>();
        parts.add("- Tên: " + trip.getName());
        parts.add("- Điểm đến: " + trip.getDestination().getName() + " (" + trip.getDestination().getCountry() + ")");
        parts.add("- Thời gian: " + trip.getStartDate().format(DATE) + " → " + trip.getEndDate().format(DATE)
                + " (" + trip.lengthInDays() + " ngày)");
        parts.add("- Số người: " + trip.getTravelers());
        if (trip.getBudget() != null) {
            parts.add("- Ngân sách: " + trip.getBudget() + " " + trip.getCurrency());
        }
        if (trip.getTravelStyle() != null) {
            parts.add("- Phong cách: " + trip.getTravelStyle());
        }
        if (trip.getPreferences() != null && !trip.getPreferences().isEmpty()) {
            parts.add("- Sở thích: " + String.join(", ", trip.getPreferences()));
        }
        TravelPlan plan = trip.getTravelPlan();
        if (plan != null) {
            if (plan.hasOrigin()) {
                parts.add("- Xuất phát từ: " + plan.origin().strip());
            }
            if (plan.transportMode() != null) {
                parts.add("- Phương tiện đến/về: " + TRANSPORT.get(plan.transportMode()));
            }
            if (plan.arrivalTime() != null) {
                parts.add("- Giờ có mặt tại điểm đến ngày đầu: " + plan.arrivalTime());
            }
            if (plan.departureTime() != null) {
                parts.add("- Giờ rời điểm đến ngày cuối: " + plan.departureTime());
            }
            if (plan.localTransport() != null) {
                parts.add("- Di chuyển tại chỗ: " + LOCAL.get(plan.localTransport()));
            }
            if (plan.lodgingType() != null || plan.lodgingName() != null) {
                parts.add("- Nơi ở: " + (plan.lodgingType() == null ? "" : LODGING.get(plan.lodgingType()))
                        + (plan.lodgingName() == null || plan.lodgingName().isBlank() ? "" : " " + plan.lodgingName().strip()));
            }
            if (plan.budgetIncludesTransport() != null) {
                parts.add("- Ngân sách " + (plan.budgetIncludesTransport() ? "đã gồm" : "không gồm") + " vé đi/về");
            }
            if (plan.avoid() != null && !plan.avoid().isBlank()) {
                parts.add("- Không muốn: " + plan.avoid().strip());
            }
        }
        return String.join("\n", parts);
    }

    /** Một dòng tóm tắt nhóm đi, ví dụ "Nhóm 4 người, 1 trẻ nhỏ, ăn chay. Tránh lịch dày và chặng đi bộ dài." */
    public String groupSummary(TripEntity trip) {
        GroupProfile group = trip.getGroupProfile();
        if (group == null || group.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        parts.add("Nhóm " + trip.getTravelers() + " người");
        if (group.childrenCount() > 0) {
            parts.add(group.childrenCount() + " trẻ nhỏ");
        }
        if (group.seniorsCount() > 0) {
            parts.add(group.seniorsCount() + " người lớn tuổi");
        }
        if (group.childAges() != null && !group.childAges().isEmpty()) {
            parts.add("tuổi trẻ: " + group.childAges().stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(", ")));
        }
        if (Boolean.TRUE.equals(group.limitedMobility())) {
            parts.add("có người đi lại khó khăn");
        }
        if (Boolean.TRUE.equals(group.motionSickness())) {
            parts.add("có người say xe (tránh đường đèo dài)");
        }
        if (group.allergies() != null && !group.allergies().isBlank()) {
            parts.add("dị ứng/kiêng: " + group.allergies().strip());
        }
        group.dietaryOrEmpty().forEach(d -> parts.add(DIETS.get(d)));
        String sentence = String.join(", ", parts) + ".";
        if (group.needsGentlePace()) {
            sentence += " Tránh lịch dày và chặng đi bộ dài.";
        }
        if (group.note() != null && !group.note().isBlank()) {
            sentence += " Ghi chú của người dùng: " + group.note().strip();
        }
        return sentence;
    }
}
