package com.tripmind.services;

import com.tripmind.domains.requests.PlaceRatingRequest;
import com.tripmind.domains.responses.PlaceRatingResponse;
import com.tripmind.domains.responses.PlaceResponse;
import com.tripmind.entities.ActivityEntity;
import com.tripmind.entities.PlaceEntity;
import com.tripmind.entities.PlaceRatingEntity;
import com.tripmind.entities.TripEntity;
import com.tripmind.enums.ActivityStatus;
import com.tripmind.enums.TripPhase;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import com.tripmind.repositories.ActivityRepository;
import com.tripmind.repositories.PlaceRatingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Đánh giá địa điểm ở tab "Nhìn lại" của chuyến đã kết thúc. Mỗi người một ý kiến cho một chỗ
 * (đánh giá lại thì ghi đè). Trợ lý đọc bảng này để không gợi ý lại chỗ đã chê.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceRatingService {

    private final PlaceRatingRepository ratingRepository;
    private final ActivityRepository activityRepository;
    private final TripService tripService;
    private final TripClock tripClock;

    /** Các địa điểm có trong lịch trình chuyến (trừ hoạt động đã bỏ), kèm đánh giá hiện tại. */
    @Transactional(readOnly = true)
    public List<PlaceRatingResponse> reviewPlaces(Long userId, Long tripId) {
        tripService.getOwnedTrip(userId, tripId);
        Map<Long, PlaceEntity> places = new LinkedHashMap<>();
        Map<Long, Set<Integer>> days = new HashMap<>();
        for (ActivityEntity a : activityRepository.findWithPlacesByTripId(tripId)) {
            if (a.getPlace() == null || a.getStatus() == ActivityStatus.SKIPPED) {
                continue;
            }
            places.putIfAbsent(a.getPlace().getId(), a.getPlace());
            days.computeIfAbsent(a.getPlace().getId(), k -> new TreeSet<>()).add((int) a.getItineraryDay().getDayNumber());
        }
        List<PlaceRatingResponse> result = new ArrayList<>();
        for (PlaceEntity place : places.values()) {
            Optional<PlaceRatingEntity> rating = ratingRepository.findByUserIdAndPlaceId(userId, place.getId());
            result.add(PlaceRatingResponse.builder()
                    .place(PlaceResponse.fromEntity(place))
                    .verdict(rating.map(PlaceRatingEntity::getVerdict).orElse(null))
                    .note(rating.map(PlaceRatingEntity::getNote).orElse(null))
                    .tripId(tripId)
                    .dayNumbers(new ArrayList<>(days.get(place.getId())))
                    .updatedAt(rating.map(PlaceRatingEntity::getUpdatedAt).orElse(null))
                    .build());
        }
        return result;
    }

    @Transactional
    public PlaceRatingResponse rate(Long userId, Long tripId, Long placeId, PlaceRatingRequest request) {
        TripEntity trip = tripService.getOwnedTrip(userId, tripId);
        if (tripClock.phase(trip) != TripPhase.AFTER) {
            throw new AppException(ErrorCode.TRIP_NOT_ENDED, "Places can be rated once the trip has ended");
        }
        PlaceEntity place = placeInTrip(tripId, placeId);
        PlaceRatingEntity rating = ratingRepository.findByUserIdAndPlaceId(userId, placeId)
                .orElseGet(() -> PlaceRatingEntity.builder().userId(userId).place(place).build());
        rating.setTripId(tripId);
        rating.setVerdict(request.getVerdict());
        rating.setNote(request.getNote() == null || request.getNote().isBlank() ? null : request.getNote().strip());
        PlaceRatingEntity saved = ratingRepository.save(rating);
        log.info("User ID={} rated place ID={} {}", userId, placeId, saved.getVerdict());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long userId, Long tripId, Long placeId) {
        tripService.getOwnedTrip(userId, tripId);
        ratingRepository.findByUserIdAndPlaceId(userId, placeId).ifPresent(ratingRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<PlaceRatingResponse> myRatings(Long userId) {
        return ratingRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    private PlaceEntity placeInTrip(Long tripId, Long placeId) {
        return activityRepository.findWithPlacesByTripId(tripId).stream()
                .map(ActivityEntity::getPlace)
                .filter(p -> p != null && p.getId().equals(placeId))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Place " + placeId + " is not in this trip's itinerary"));
    }

    private PlaceRatingResponse toResponse(PlaceRatingEntity r) {
        return PlaceRatingResponse.builder()
                .place(PlaceResponse.fromEntity(r.getPlace()))
                .verdict(r.getVerdict())
                .note(r.getNote())
                .tripId(r.getTripId())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
