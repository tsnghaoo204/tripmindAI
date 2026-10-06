package com.tripmind.services.ai;

import com.tripmind.domains.models.GroupProfile;
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
        if (Boolean.TRUE.equals(group.limitedMobility())) {
            parts.add("có người đi lại khó khăn");
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
