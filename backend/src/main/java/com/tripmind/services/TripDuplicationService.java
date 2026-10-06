package com.tripmind.services;

import com.tripmind.domains.requests.DuplicateTripRequest;
import com.tripmind.domains.responses.DuplicateTripResponse;
import com.tripmind.entities.*;
import com.tripmind.enums.ActivityCreator;
import com.tripmind.enums.ActivityStatus;
import com.tripmind.repositories.ActivityRepository;
import com.tripmind.repositories.ChecklistItemRepository;
import com.tripmind.repositories.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * "Năm ngoái đi Đà Lạt, năm nay đi lại": sao chép một chuyến sang khoảng ngày mới.
 *
 * <p>Giữ: điểm đến, ngân sách, sở thích, thông tin nhóm, ghi chú từng ngày, hoạt động (đặt lại
 * về chưa làm, coi như người dùng tự thêm), checklist (bỏ tick, dời hạn theo ngày đi mới).
 * Không mang theo: chi tiêu, hội thoại, đề xuất, đánh giá.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TripDuplicationService {

    private final TripService tripService;
    private final TripRepository tripRepository;
    private final ActivityRepository activityRepository;
    private final ChecklistItemRepository checklistRepository;
    private final TripMapper tripMapper;

    @Transactional
    public DuplicateTripResponse duplicate(Long userId, Long tripId, DuplicateTripRequest request) {
        TripEntity source = tripService.getOwnedTrip(userId, tripId);
        LocalDate start = request.getStartDate();
        long shift = ChronoUnit.DAYS.between(source.getStartDate(), start);

        TripEntity copy = TripEntity.builder()
                .user(source.getUser())
                .destination(source.getDestination())
                .name(request.getName() != null && !request.getName().isBlank() ? request.getName().strip()
                        : truncate(source.getName() + " (bản sao)", 160))
                .startDate(start)
                .endDate(source.getEndDate().plusDays(shift))
                .travelers(source.getTravelers())
                .budget(source.getBudget())
                .currency(source.getCurrency())
                .travelStyle(source.getTravelStyle())
                .budgetPreference(source.getBudgetPreference())
                .preferences(new ArrayList<>(source.getPreferences()))
                .groupProfile(source.getGroupProfile())
                .days(new ArrayList<>())
                .build();

        List<DuplicateTripResponse.SkippedActivity> skipped = new ArrayList<>();
        int activities = 0;
        for (ItineraryDayEntity day : source.getDays()) {
            ItineraryDayEntity newDay = ItineraryDayEntity.builder()
                    .trip(copy)
                    .dayNumber(day.getDayNumber())
                    .date(day.getDate().plusDays(shift))
                    .note(day.getNote())
                    .activities(new ArrayList<>())
                    .build();
            for (ActivityEntity a : activityRepository.findByItineraryDayIdOrderByOrderIndexAsc(day.getId())) {
                if (a.getStatus() == ActivityStatus.SKIPPED) {
                    skipped.add(new DuplicateTripResponse.SkippedActivity(null, (int) day.getDayNumber(), a.getTitle(),
                            a.getSkipReason() == null ? null : a.getSkipReason().name()));
                }
                newDay.getActivities().add(ActivityEntity.builder()
                        .itineraryDay(newDay)
                        .title(a.getTitle())
                        .place(a.getPlace())
                        .activityType(a.getActivityType())
                        .createdBy(ActivityCreator.USER)
                        .startTime(a.getStartTime())
                        .endTime(a.getEndTime())
                        .estimatedCost(a.getEstimatedCost())
                        .estimatedCostSource(a.getEstimatedCostSource())
                        .transportationMode(a.getTransportationMode())
                        .notes(a.getNotes())
                        .orderIndex(a.getOrderIndex())
                        .status(ActivityStatus.PLANNED)
                        .build());
                activities++;
            }
            copy.getDays().add(newDay);
        }
        copy = tripRepository.save(copy);

        int checklist = 0;
        for (ChecklistItemEntity item : checklistRepository.findByTripIdOrderByKindAscOrderIndexAscIdAsc(tripId)) {
            checklistRepository.save(ChecklistItemEntity.builder()
                    .trip(copy)
                    .kind(item.getKind())
                    .title(item.getTitle())
                    .category(item.getCategory())
                    .dueDate(item.getDueDate() == null ? null : item.getDueDate().plusDays(shift))
                    .done(false)
                    .source(item.getSource())
                    .reason(item.getReason())
                    .orderIndex(item.getOrderIndex())
                    .build());
            checklist++;
        }

        // Gán mã mới cho các hoạt động từng bị bỏ, để giao diện trỏ thẳng tới chúng.
        List<DuplicateTripResponse.SkippedActivity> skippedWithIds = new ArrayList<>();
        for (DuplicateTripResponse.SkippedActivity s : skipped) {
            Long id = copy.getDays().stream()
                    .filter(d -> d.getDayNumber() == s.dayNumber())
                    .flatMap(d -> d.getActivities().stream())
                    .filter(a -> a.getTitle().equals(s.title()))
                    .map(ActivityEntity::getId)
                    .findFirst().orElse(null);
            skippedWithIds.add(new DuplicateTripResponse.SkippedActivity(id, s.dayNumber(), s.title(), s.skipReason()));
        }
        log.info("Duplicated trip ID={} into ID={} ({} activities, {} checklist items)", tripId, copy.getId(),
                activities, checklist);
        return DuplicateTripResponse.builder()
                .trip(tripMapper.toDetail(copy))
                .activitiesCopied(activities)
                .checklistItemsCopied(checklist)
                .previouslySkipped(skippedWithIds)
                .build();
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
