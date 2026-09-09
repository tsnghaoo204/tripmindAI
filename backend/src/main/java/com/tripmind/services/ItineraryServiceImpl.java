package com.tripmind.services;

import com.tripmind.domains.requests.CreateActivityRequest;
import com.tripmind.domains.requests.UpdateActivityRequest;
import com.tripmind.domains.responses.ActivityResponse;
import com.tripmind.domains.responses.ItineraryDayResponse;
import com.tripmind.domains.responses.ItineraryResponse;
import com.tripmind.domains.responses.PlaceResponse;
import com.tripmind.entities.ActivityEntity;
import com.tripmind.entities.ItineraryDayEntity;
import com.tripmind.entities.PlaceEntity;
import com.tripmind.entities.TripEntity;
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

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItineraryServiceImpl implements ItineraryService {

    private final TripRepository tripRepository;
    private final ItineraryDayRepository itineraryDayRepository;
    private final ActivityRepository activityRepository;
    private final PlaceRepository placeRepository;
    private final DistanceService distanceService;

    @Override
    @Transactional(readOnly = true)
    public ItineraryResponse getItinerary(Long userId, Long tripId) {
        TripEntity trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Trip not found with id: " + tripId));

        if (!trip.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Access denied: You do not own this trip");
        }

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
                if (act.getEstimatedCost() != null) {
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
        TripEntity trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Trip not found with id: " + tripId));

        if (!trip.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Access denied: You do not own this trip");
        }

        ItineraryDayEntity targetDay;
        if (request.getDayId() != null) {
            targetDay = itineraryDayRepository.findById(request.getDayId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Itinerary day not found with id: " + request.getDayId()));
            if (!targetDay.getTrip().getId().equals(tripId)) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "The specified day does not belong to trip " + tripId);
            }
        } else if (request.getDayNumber() != null) {
            targetDay = itineraryDayRepository.findByTripIdAndDayNumber(tripId, request.getDayNumber().shortValue())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Day number " + request.getDayNumber() + " not found for trip: " + tripId));
        } else {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Either dayId or dayNumber must be provided");
        }

        PlaceEntity place = null;
        if (request.getPlaceId() != null) {
            place = placeRepository.findById(request.getPlaceId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Place not found with id: " + request.getPlaceId()));
        }

        Short maxOrder = activityRepository.findMaxOrderIndexByItineraryDayId(targetDay.getId());
        short nextOrder = (short) (maxOrder == null ? 0 : maxOrder + 1);

        ActivityEntity activity = ActivityEntity.builder()
                .itineraryDay(targetDay)
                .title(request.getTitle())
                .place(place)
                .activityType(request.getActivityType())
                .createdBy("USER")
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .estimatedCost(request.getEstimatedCost() != null ? request.getEstimatedCost() : 0L)
                .transportationMode(request.getTransportationMode() != null ? request.getTransportationMode() : "MOTORBIKE")
                .notes(request.getNotes())
                .orderIndex(nextOrder)
                .status("PLANNED")
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
            activity.setTitle(request.getTitle());
        }
        if (request.getActivityType() != null) {
            activity.setActivityType(request.getActivityType());
        }
        if (request.getPlaceId() != null) {
            PlaceEntity place = placeRepository.findById(request.getPlaceId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Place not found with id: " + request.getPlaceId()));
            activity.setPlace(place);
        }
        if (request.getStartTime() != null) {
            activity.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            activity.setEndTime(request.getEndTime());
        }
        if (request.getEstimatedCost() != null) {
            activity.setEstimatedCost(request.getEstimatedCost());
        }
        if (request.getTransportationMode() != null) {
            activity.setTransportationMode(request.getTransportationMode());
        }
        if (request.getNotes() != null) {
            activity.setNotes(request.getNotes());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            activity.setStatus(request.getStatus().trim().toUpperCase());
        }
        if (request.getSkipReason() != null) {
            activity.setSkipReason(request.getSkipReason());
        }

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
        if (existing.size() != activityIds.size()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Activity IDs list size (" + activityIds.size() + ") does not match current day activities count (" + existing.size() + ")");
        }

        Map<Long, ActivityEntity> map = new HashMap<>();
        for (ActivityEntity act : existing) {
            map.put(act.getId(), act);
        }

        for (Long id : activityIds) {
            if (!map.containsKey(id)) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "Activity ID " + id + " does not belong to this day");
            }
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
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .estimatedCost(entity.getEstimatedCost())
                .transportationMode(entity.getTransportationMode())
                .notes(entity.getNotes())
                .status(entity.getStatus())
                .place(PlaceResponse.fromEntity(entity.getPlace()))
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
