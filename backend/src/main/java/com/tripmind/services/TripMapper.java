package com.tripmind.services;

import com.tripmind.configurations.properties.TripProperties;
import com.tripmind.domains.models.GroupProfile;
import com.tripmind.domains.responses.DestinationResponse;
import com.tripmind.domains.responses.TripResponse;
import com.tripmind.entities.TripEntity;
import com.tripmind.repositories.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Dựng {@link TripResponse}, kèm giai đoạn và tiến độ lập kế hoạch tính tại chỗ (không lưu cột). */
@Component
@RequiredArgsConstructor
public class TripMapper {

    private final TripRepository tripRepository;
    private final TripClock tripClock;
    private final TripProperties tripProperties;

    public TripResponse toDetail(TripEntity trip) {
        TripResponse response = toSummary(trip, plannedDays(List.of(trip)).getOrDefault(trip.getId(), 0L));
        response.setDays(trip.getDays().stream()
                .map(day -> TripResponse.DaySummary.builder()
                        .id(day.getId())
                        .dayNumber(day.getDayNumber())
                        .date(day.getDate())
                        .note(day.getNote())
                        .activityCount(day.getActivities().size())
                        .build())
                .toList());
        return response;
    }

    public List<TripResponse> toSummaries(List<TripEntity> trips) {
        Map<Long, Long> planned = plannedDays(trips);
        return trips.stream()
                .map(trip -> toSummary(trip, planned.getOrDefault(trip.getId(), 0L)))
                .toList();
    }

    private TripResponse toSummary(TripEntity trip, long plannedDays) {
        int totalDays = trip.lengthInDays();
        return TripResponse.builder()
                .id(trip.getId())
                .name(trip.getName())
                .destination(DestinationResponse.fromEntity(trip.getDestination()))
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .totalDays(totalDays)
                .travelers(trip.getTravelers())
                .budget(trip.getBudget())
                .currency(trip.getCurrency())
                .travelStyle(trip.getTravelStyle())
                .budgetPreference(trip.getBudgetPreference())
                .preferences(trip.getPreferences())
                .groupProfile(trip.getGroupProfile() == null ? GroupProfile.empty() : trip.getGroupProfile())
                .phase(tripClock.phase(trip))
                .phaseSource(trip.getPhaseOverride() == null ? "AUTO" : "MANUAL")
                .today(tripClock.today(trip))
                .planningProgress(totalDays == 0 ? 0 : (int) Math.round(plannedDays * 100.0 / totalDays))
                .createdAt(trip.getCreatedAt())
                .updatedAt(trip.getUpdatedAt())
                .build();
    }

    private Map<Long, Long> plannedDays(List<TripEntity> trips) {
        Map<Long, Long> result = new HashMap<>();
        if (trips.isEmpty() || trips.stream().anyMatch(t -> t.getId() == null)) {
            return result;
        }
        List<Long> ids = trips.stream().map(TripEntity::getId).toList();
        for (Object[] row : tripRepository.countPlannedDays(ids, tripProperties.plannedDayMinActivities())) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }
}
