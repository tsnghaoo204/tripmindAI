package com.tripmind.services;

import com.tripmind.domains.requests.CreateActivityRequest;
import com.tripmind.domains.requests.UpdateActivityRequest;
import com.tripmind.domains.responses.ActivityResponse;
import com.tripmind.domains.responses.CostEstimateResponse;
import com.tripmind.domains.responses.ItineraryDayResponse;
import com.tripmind.domains.responses.ItineraryResponse;
import com.tripmind.domains.responses.PlaceResponse;
import com.tripmind.entities.ActivityEntity;
import com.tripmind.entities.ItineraryDayEntity;
import com.tripmind.entities.PlaceEntity;
import com.tripmind.entities.TripEntity;
import com.tripmind.enums.ActivityCreator;
import com.tripmind.enums.ActivityStatus;
import com.tripmind.enums.ActivityType;
import com.tripmind.enums.CostSource;
import com.tripmind.enums.PlaceAdoption;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import com.tripmind.repositories.ActivityRepository;
import com.tripmind.repositories.ItineraryDayRepository;
import com.tripmind.repositories.PlaceRepository;
import com.tripmind.repositories.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItineraryServiceImpl implements ItineraryService {

    private static final String DEFAULT_TRANSPORT = "MOTORBIKE";

    private final TripRepository tripRepository;
    private final ItineraryDayRepository itineraryDayRepository;
    private final ActivityRepository activityRepository;
    private final PlaceRepository placeRepository;
    private final DistanceService distanceService;
    private final PlaceAdoptionService placeAdoptionService;
    private final CostEstimator costEstimator;

    @Override
    @Transactional(readOnly = true)
    public ItineraryResponse getItinerary(Long userId, Long tripId) {
        TripEntity trip = ownedTrip(userId, tripId);

        List<ItineraryDayEntity> days = itineraryDayRepository.findByTripIdOrderByDayNumberAsc(tripId);

        List<ItineraryDayResponse> dayResponses = new ArrayList<>();
        long totalTripDistance = 0L;
        int totalActivities = 0;
        long totalEstimatedCost = 0L;

        for (ItineraryDayEntity day : days) {
            List<ActivityEntity> activities = activityRepository.findByItineraryDayIdOrderByOrderIndexAsc(day.getId());
            ItineraryDayResponse dayResponse = buildDayResponse(day, activities);
            dayResponses.add(dayResponse);

            if (dayResponse.getTotalDayDistanceMeters() != null) {
                totalTripDistance += dayResponse.getTotalDayDistanceMeters();
            }
            totalActivities += activities.size();
            for (ActivityEntity act : activities) {
                if (act.getEstimatedCost() != null && act.getStatus() != ActivityStatus.SKIPPED) {
                    totalEstimatedCost += act.getEstimatedCost();
                }
            }
        }

        return ItineraryResponse.builder()
                .tripId(trip.getId())
                .tripName(trip.getName())
                .destinationName(trip.getDestination() != null ? trip.getDestination().getName() : null)
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .totalDays(days.size())
                .totalActivities(totalActivities)
                .totalEstimatedCost(totalEstimatedCost)
                .currency(trip.getCurrency())
                .days(dayResponses)
                .totalTripDistanceMeters(totalTripDistance)
                .formattedTotalTripDistance(distanceService.formatDistance(totalTripDistance))
                .build();
    }

    @Override
    @Transactional
    public ActivityResponse addActivity(Long userId, Long tripId, CreateActivityRequest request) {
        ownedTrip(userId, tripId);

        ItineraryDayEntity targetDay;
        if (request.getDayId() != null) {
            targetDay = itineraryDayRepository.findById(request.getDayId())
                    .filter(day -> day.getTrip().getId().equals(tripId))
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                            "Itinerary day " + request.getDayId() + " not found in trip " + tripId));
        } else if (request.getDayNumber() != null) {
            targetDay = itineraryDayRepository.findByTripIdAndDayNumber(tripId, request.getDayNumber().shortValue())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Day number " + request.getDayNumber() + " not found for trip: " + tripId));
        } else {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Either dayId or dayNumber must be provided");
        }

        validateTimeRange(request.getStartTime(), request.getEndTime());

        PlaceEntity place = placeAdoptionService.resolve(
                request.getPlaceId(), request.getPlaceProvider(), request.getPlaceExternalId(), PlaceAdoption.ITINERARY);

        Short maxOrder = activityRepository.findMaxOrderIndexByItineraryDayId(targetDay.getId());
        short nextOrder = (short) (maxOrder == null ? 0 : maxOrder + 1);

        ActivityEntity activity = ActivityEntity.builder()
                .itineraryDay(targetDay)
                .title(request.getTitle().trim())
                .place(place)
                .activityType(request.getActivityType())
                .createdBy(ActivityCreator.USER)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .estimatedCost(request.getEstimatedCost())
                .estimatedCostSource(costSource(request.getEstimatedCost(), request.getEstimatedCostSource()))
                .transportationMode(request.getTransportationMode() != null ? request.getTransportationMode() : DEFAULT_TRANSPORT)
                .notes(request.getNotes())
                .orderIndex(nextOrder)
                .status(ActivityStatus.PLANNED)
                .build();

        ActivityEntity saved = activityRepository.save(activity);
        log.info("Added activity '{}' to trip ID={} day #{} with orderIndex={}", saved.getTitle(), tripId, targetDay.getDayNumber(), nextOrder);

        return toActivityResponse(saved, null);
    }

    @Override
    @Transactional
    public ActivityResponse updateActivity(Long userId, Long activityId, UpdateActivityRequest request) {
        ActivityEntity activity = activityRepository.findByIdAndUserId(activityId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Activity not found or access denied: " + activityId));

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            activity.setTitle(request.getTitle().trim());
        }
        if (request.getActivityType() != null) {
            activity.setActivityType(request.getActivityType());
        }
        if (request.getPlaceId() != null || request.getPlaceExternalId() != null) {
            activity.setPlace(placeAdoptionService.resolve(
                    request.getPlaceId(), request.getPlaceProvider(), request.getPlaceExternalId(), PlaceAdoption.ITINERARY));
        }
        LocalTime start = request.getStartTime() != null ? request.getStartTime() : activity.getStartTime();
        LocalTime end = request.getEndTime() != null ? request.getEndTime() : activity.getEndTime();
        validateTimeRange(start, end);
        activity.setStartTime(start);
        activity.setEndTime(end);

        if (request.getEstimatedCost() != null) {
            activity.setEstimatedCost(request.getEstimatedCost());
            activity.setEstimatedCostSource(costSource(request.getEstimatedCost(), request.getEstimatedCostSource()));
        }
        if (request.getTransportationMode() != null) {
            activity.setTransportationMode(request.getTransportationMode());
        }
        if (request.getNotes() != null) {
            activity.setNotes(request.getNotes());
        }
        applyStatus(activity, request);

        ActivityEntity updated = activityRepository.save(activity);
        log.info("Updated activity ID={} for user ID={}", activityId, userId);

        return toActivityResponse(updated, null);
    }

    @Override
    @Transactional
    public void deleteActivity(Long userId, Long activityId) {
        ActivityEntity activity = activityRepository.findByIdAndUserId(activityId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Activity not found or access denied: " + activityId));

        ItineraryDayEntity day = activity.getItineraryDay();
        activityRepository.delete(activity);
        activityRepository.flush();

        // Re-index remaining activities
        List<ActivityEntity> remaining = activityRepository.findByItineraryDayIdOrderByOrderIndexAsc(day.getId());
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setOrderIndex((short) i);
        }
        activityRepository.saveAll(remaining);
        log.info("Deleted activity ID={} and reindexed remaining {} activities", activityId, remaining.size());
    }

    @Override
    @Transactional
    public ItineraryDayResponse reorderActivities(Long userId, Long dayId, List<Long> activityIds) {
        ItineraryDayEntity day = itineraryDayRepository.findByIdAndUserId(dayId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Itinerary day not found or access denied: " + dayId));

        List<ActivityEntity> existing = activityRepository.findByItineraryDayIdOrderByOrderIndexAsc(dayId);
        Map<Long, ActivityEntity> map = new HashMap<>();
        for (ActivityEntity act : existing) {
            map.put(act.getId(), act);
        }

        // BR-205: danh sách gửi lên phải là đúng tập hoạt động của ngày — không thiếu, không thừa, không trùng.
        Set<Long> requested = new HashSet<>(activityIds);
        if (requested.size() != activityIds.size() || !requested.equals(map.keySet())) {
            throw new AppException(ErrorCode.REORDER_SET_MISMATCH,
                    "Reorder list must contain each activity of the day exactly once",
                    Map.of("expected", map.keySet(), "received", activityIds));
        }

        // Pass 1: Set temporary offset to avoid intermediate unique index collisions
        for (int i = 0; i < activityIds.size(); i++) {
            ActivityEntity act = map.get(activityIds.get(i));
            act.setOrderIndex((short) (1000 + i));
        }
        activityRepository.saveAll(existing);
        activityRepository.flush();

        // Pass 2: Set actual target indices (0, 1, 2...)
        List<ActivityEntity> reorderedList = new ArrayList<>();
        for (int i = 0; i < activityIds.size(); i++) {
            ActivityEntity act = map.get(activityIds.get(i));
            act.setOrderIndex((short) i);
            reorderedList.add(act);
        }
        activityRepository.saveAll(reorderedList);
        activityRepository.flush();

        log.info("Reordered {} activities for day ID={}", activityIds.size(), dayId);
        return buildDayResponse(day, reorderedList);
    }

    @Override
    @Transactional(readOnly = true)
    public CostEstimateResponse estimateCost(Long userId, Long tripId, Long placeId, Integer priceLevel,
                                             ActivityType activityType) {
        TripEntity trip = ownedTrip(userId, tripId);
        Integer level = priceLevel;
        if (placeId != null) {
            PlaceEntity place = placeRepository.findById(placeId)
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Place not found with id: " + placeId));
            level = place.getPriceLevel() == null ? null : place.getPriceLevel().intValue();
        }
        return costEstimator.estimate(level, activityType, trip.getTravelers(), trip.getCurrency());
    }

    private TripEntity ownedTrip(Long userId, Long tripId) {
        return tripRepository.findByIdAndUserId(tripId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Trip not found or access denied"));
    }

    private void validateTimeRange(LocalTime start, LocalTime end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new AppException(ErrorCode.INVALID_TIME_RANGE,
                    "End time " + end + " cannot be before start time " + start);
        }
    }

    private CostSource costSource(Long cost, CostSource requested) {
        if (cost == null) {
            return null;
        }
        return requested == CostSource.PRICE_LEVEL ? CostSource.PRICE_LEVEL : CostSource.USER;
    }

    /**
     * Đánh dấu thực hiện (FR-1003 → FR-1006). Bỏ một hoạt động phải có lý do; trong một chuyến
     * chỉ có tối đa một hoạt động đang làm; giờ thực tế ghi cạnh giờ dự kiến, không thay thế.
     */
    private void applyStatus(ActivityEntity activity, UpdateActivityRequest request) {
        ActivityStatus status = request.getStatus() != null ? request.getStatus() : activity.getStatus();
        if (status == ActivityStatus.SKIPPED) {
            var reason = request.getSkipReason() != null ? request.getSkipReason() : activity.getSkipReason();
            if (reason == null) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "skipReason is required when status is SKIPPED");
            }
            activity.setSkipReason(reason);
        } else {
            activity.setSkipReason(null);
        }
        if (status == ActivityStatus.DOING && activity.getStatus() != ActivityStatus.DOING) {
            Long tripId = activity.getItineraryDay().getTrip().getId();
            activityRepository.findFirstByItineraryDayTripIdAndStatusAndIdNot(tripId, ActivityStatus.DOING, activity.getId())
                    .ifPresent(other -> {
                        throw new AppException(ErrorCode.CONFLICT,
                                "Activity '" + other.getTitle() + "' is already in progress",
                                Map.of("activityId", other.getId()));
                    });
        }
        activity.setStatus(status);

        LocalTime actualStart = request.getActualStart() != null ? request.getActualStart() : activity.getActualStart();
        LocalTime actualEnd = request.getActualEnd() != null ? request.getActualEnd() : activity.getActualEnd();
        validateTimeRange(actualStart, actualEnd);
        activity.setActualStart(actualStart);
        activity.setActualEnd(actualEnd);
    }

    private ItineraryDayResponse buildDayResponse(ItineraryDayEntity day, List<ActivityEntity> activities) {
        List<ActivityResponse> actResponses = new ArrayList<>();
        long dayDistance = 0L;
        int dayTravelMinutes = 0;

        for (int i = 0; i < activities.size(); i++) {
            ActivityEntity current = activities.get(i);
            DistanceService.TravelEstimate estimate = null;

            if (i < activities.size() - 1) {
                ActivityEntity next = activities.get(i + 1);
                if (current.getPlace() != null && next.getPlace() != null) {
                    estimate = distanceService.estimateTravel(
                            current.getPlace(),
                            next.getPlace(),
                            next.getTransportationMode()
                    );
                }
            }

            ActivityResponse response = toActivityResponse(current, estimate);
            actResponses.add(response);

            if (estimate != null && estimate.getDistanceMeters() != null) {
                dayDistance += estimate.getDistanceMeters();
                dayTravelMinutes += estimate.getTravelTimeMinutes() != null ? estimate.getTravelTimeMinutes() : 0;
            }
        }

        return ItineraryDayResponse.builder()
                .id(day.getId())
                .tripId(day.getTrip().getId())
                .dayNumber(day.getDayNumber())
                .date(day.getDate())
                .note(day.getNote())
                .activities(actResponses)
                .totalDayDistanceMeters(dayDistance)
                .formattedTotalDayDistance(distanceService.formatDistance(dayDistance))
                .totalDayTravelMinutes(dayTravelMinutes)
                .formattedTotalDayTravelTime(distanceService.formatDuration(dayTravelMinutes))
                .build();
    }

    private ActivityResponse toActivityResponse(ActivityEntity entity, DistanceService.TravelEstimate estimate) {
        String idealTip = distanceService.getIdealTimingTip(entity.getPlace(), entity.getTitle());

        ActivityResponse.ActivityResponseBuilder builder = ActivityResponse.builder()
                .id(entity.getId())
                .dayId(entity.getItineraryDay().getId())
                .dayNumber(entity.getItineraryDay().getDayNumber())
                .orderIndex(entity.getOrderIndex())
                .title(entity.getTitle())
                .activityType(entity.getActivityType())
                .createdBy(entity.getCreatedBy())
                .fromProposalId(entity.getFromProposalId())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .estimatedCost(entity.getEstimatedCost())
                .estimatedCostSource(entity.getEstimatedCostSource())
                .transportationMode(entity.getTransportationMode())
                .notes(entity.getNotes())
                .status(entity.getStatus())
                .skipReason(entity.getSkipReason())
                .actualStart(entity.getActualStart())
                .actualEnd(entity.getActualEnd())
                .place(PlaceResponse.fromEntity(entity.getPlace()))
                .placeName(entity.getPlace() != null ? entity.getPlace().getName() : null)
                .idealTimingTip(idealTip);

        if (estimate != null) {
            builder.distanceToNextMeters(estimate.getDistanceMeters())
                    .formattedDistanceToNext(estimate.getFormattedDistance())
                    .travelTimeToNextMinutes(estimate.getTravelTimeMinutes())
                    .formattedTravelTimeToNext(estimate.getFormattedTravelTime());
        }

        return builder.build();
    }
}
