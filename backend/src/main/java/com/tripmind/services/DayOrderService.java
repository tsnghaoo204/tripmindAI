package com.tripmind.services;

import com.tripmind.entities.ActivityEntity;
import com.tripmind.entities.ItineraryDayEntity;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import com.tripmind.repositories.ActivityRepository;
import com.tripmind.repositories.ItineraryDayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Đọc một ngày của chuyến rồi chạy {@link ItineraryOptimizer}. Không ghi gì. */
@Service
@RequiredArgsConstructor
public class DayOrderService {

    private final TripService tripService;
    private final ItineraryDayRepository dayRepository;
    private final ActivityRepository activityRepository;
    private final ItineraryOptimizer optimizer;

    public record DayOrder(List<Long> currentOrder, ItineraryOptimizer.Result result) {
    }

    @Transactional(readOnly = true)
    public DayOrder optimize(Long userId, Long tripId, int dayNumber) {
        tripService.getOwnedTrip(userId, tripId);
        ItineraryDayEntity day = dayRepository.findByTripIdAndDayNumber(tripId, (short) dayNumber)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Day " + dayNumber + " not found"));
        List<ActivityEntity> activities = activityRepository.findByItineraryDayIdOrderByOrderIndexAsc(day.getId());
        List<ItineraryOptimizer.Stop> stops = activities.stream()
                .map(a -> new ItineraryOptimizer.Stop(a.getId(),
                        a.getPlace() == null ? null : a.getPlace().getLatitude().doubleValue(),
                        a.getPlace() == null ? null : a.getPlace().getLongitude().doubleValue(),
                        a.getStartTime() != null))
                .toList();
        return new DayOrder(stops.stream().map(ItineraryOptimizer.Stop::id).toList(), optimizer.optimize(stops));
    }
}
