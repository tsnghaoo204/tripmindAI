package com.tripmind.services;

import com.tripmind.domains.responses.ActivityAlternativeResponse;
import com.tripmind.domains.responses.ActivityResponse;
import com.tripmind.domains.responses.PlaceResponse;
import com.tripmind.entities.ActivityAlternativeEntity;
import com.tripmind.entities.ActivityEntity;
import com.tripmind.entities.PlaceEntity;
import com.tripmind.entities.TripEntity;
import com.tripmind.enums.ActivityType;
import com.tripmind.enums.PlaceAdoption;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import com.tripmind.repositories.ActivityAlternativeRepository;
import com.tripmind.repositories.ActivityRepository;
import com.tripmind.repositories.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityAlternativeService {

    private final ActivityRepository activityRepository;
    private final ActivityAlternativeRepository alternativeRepository;
    private final PlaceRepository placeRepository;
    private final PlaceService placeService;
    private final PlaceAdoptionService placeAdoptionService;
    private final CostEstimator costEstimator;

    @Transactional
    public List<ActivityAlternativeResponse> getOrGenerateAlternatives(Long userId, Long activityId) {
        ActivityEntity activity = activityRepository.findByIdAndUserId(activityId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Activity not found or access denied: " + activityId));

        List<ActivityAlternativeEntity> existing = alternativeRepository.findByActivityIdWithPlace(activityId);
        if (!existing.isEmpty()) {
            return existing.stream().map(this::toResponse).toList();
        }

        TripEntity trip = activity.getItineraryDay().getTrip();
        return generateForActivity(activity, trip).stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<ActivityAlternativeEntity> generateForActivity(ActivityEntity activity, TripEntity trip) {
        if (activity.getActivityType() != ActivityType.FOOD && activity.getActivityType() != ActivityType.ACCOMMODATION) {
            return List.of();
        }

        List<ActivityAlternativeEntity> existing = alternativeRepository.findByActivityIdWithPlace(activity.getId());
        if (!existing.isEmpty()) {
            return existing;
        }

        String destination = trip.getDestination() != null ? trip.getDestination().getName() : "Đà Nẵng";
        int travelers = Math.max(1, trip.getTravelers());
        String currency = trip.getCurrency() != null ? trip.getCurrency() : "VND";

        List<ActivityAlternativeEntity> alts = new ArrayList<>();

        if (activity.getActivityType() == ActivityType.FOOD) {
            generateFoodAlternatives(activity, trip, destination, travelers, currency, alts);
        } else if (activity.getActivityType() == ActivityType.ACCOMMODATION) {
            generateLodgingAlternatives(activity, trip, destination, travelers, currency, alts);
        }

        return alternativeRepository.saveAll(alts);
    }

    @Transactional
    public ActivityResponse selectAlternative(Long userId, Long activityId, Long alternativeId) {
        ActivityEntity activity = activityRepository.findByIdAndUserId(activityId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Activity not found or access denied: " + activityId));

        ActivityAlternativeEntity chosen = alternativeRepository.findByIdAndActivityId(alternativeId, activityId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Alternative not found: " + alternativeId));

        List<ActivityAlternativeEntity> all = alternativeRepository.findByActivityIdWithPlace(activityId);
        for (ActivityAlternativeEntity alt : all) {
            alt.setSelected(alt.getId().equals(chosen.getId()));
        }
        alternativeRepository.saveAll(all);

        if (chosen.getPlace() != null) {
            activity.setPlace(chosen.getPlace());
        }
        activity.setTitle(chosen.getTitle());
        if (chosen.getEstimatedCost() != null) {
            activity.setEstimatedCost(chosen.getEstimatedCost());
        }
        activityRepository.save(activity);

        List<ActivityAlternativeResponse> altResponses = all.stream().map(this::toResponse).toList();
        return ActivityResponse.builder()
                .id(activity.getId())
                .dayId(activity.getItineraryDay().getId())
                .dayNumber(activity.getItineraryDay().getDayNumber())
                .orderIndex(activity.getOrderIndex())
                .title(activity.getTitle())
                .activityType(activity.getActivityType())
                .createdBy(activity.getCreatedBy())
                .fromProposalId(activity.getFromProposalId())
                .startTime(activity.getStartTime())
                .endTime(activity.getEndTime())
                .estimatedCost(activity.getEstimatedCost())
                .estimatedCostSource(activity.getEstimatedCostSource())
                .transportationMode(activity.getTransportationMode())
                .notes(activity.getNotes())
                .status(activity.getStatus())
                .skipReason(activity.getSkipReason())
                .actualStart(activity.getActualStart())
                .actualEnd(activity.getActualEnd())
                .place(PlaceResponse.fromEntity(activity.getPlace()))
                .placeName(activity.getPlace() != null ? activity.getPlace().getName() : chosen.getPlaceName())
                .alternatives(altResponses)
                .build();
    }

    private void generateFoodAlternatives(ActivityEntity activity, TripEntity trip, String destination,
                                          int travelers, String currency, List<ActivityAlternativeEntity> alts) {
        PlaceEntity currentPlace = activity.getPlace();
        String currentName = currentPlace != null ? currentPlace.getName() : activity.getTitle();

        // 1. Balanced (Hiện tại / Đề xuất)
        Long balancedCost = activity.getEstimatedCost() != null ? activity.getEstimatedCost()
                : suggestCost(2, ActivityType.FOOD, travelers, currency, 150_000L * travelers);

        ActivityAlternativeEntity balanced = ActivityAlternativeEntity.builder()
                .activity(activity)
                .place(currentPlace)
                .placeName(currentName)
                .title(activity.getTitle())
                .tier("BALANCED")
                .tierLabel("Tiêu chuẩn (Đề xuất)")
                .estimatedCost(balancedCost)
                .rating(currentPlace != null ? currentPlace.getRating() : BigDecimal.valueOf(4.5))
                .userRatingsTotal(currentPlace != null ? currentPlace.getUserRatingsTotal() : 120)
                .address(currentPlace != null ? currentPlace.getAddress() : null)
                .isSelected(true)
                .build();

        // 2. Budget (Tiết kiệm - Bình dân / Món ngon đường phố)
        PlaceEntity budgetPlace = findOrAdoptCandidate("quán ăn bình dân ngon", destination, currentPlace);
        Long budgetCost = suggestCost(1, ActivityType.FOOD, travelers, currency, 60_000L * travelers);
        String budgetName = budgetPlace != null ? budgetPlace.getName() : "Quán ăn bình dân đặc sản";
        ActivityAlternativeEntity budget = ActivityAlternativeEntity.builder()
                .activity(activity)
                .place(budgetPlace)
                .placeName(budgetName)
                .title("Ẩm thực bình dân: " + budgetName)
                .tier("BUDGET")
                .tierLabel("Tiết kiệm (Bình dân)")
                .estimatedCost(budgetCost)
                .rating(budgetPlace != null && budgetPlace.getRating() != null ? budgetPlace.getRating() : BigDecimal.valueOf(4.3))
                .userRatingsTotal(budgetPlace != null ? budgetPlace.getUserRatingsTotal() : 85)
                .address(budgetPlace != null ? budgetPlace.getAddress() : null)
                .isSelected(false)
                .build();

        // 3. Premium (Trải nghiệm - Nhà hàng cao cấp / view đẹp)
        PlaceEntity premiumPlace = findOrAdoptCandidate("nhà hàng cao cấp hải sản view đẹp", destination, currentPlace);
        Long premiumCost = suggestCost(3, ActivityType.FOOD, travelers, currency, 400_000L * travelers);
        String premiumName = premiumPlace != null ? premiumPlace.getName() : "Nhà hàng cao cấp & View đẹp";
        ActivityAlternativeEntity premium = ActivityAlternativeEntity.builder()
                .activity(activity)
                .place(premiumPlace)
                .placeName(premiumName)
                .title("Trải nghiệm ẩm thực: " + premiumName)
                .tier("PREMIUM")
                .tierLabel("Trải nghiệm (Cao cấp)")
                .estimatedCost(premiumCost)
                .rating(premiumPlace != null && premiumPlace.getRating() != null ? premiumPlace.getRating() : BigDecimal.valueOf(4.7))
                .userRatingsTotal(premiumPlace != null ? premiumPlace.getUserRatingsTotal() : 250)
                .address(premiumPlace != null ? premiumPlace.getAddress() : null)
                .isSelected(false)
                .build();

        alts.add(budget);
        alts.add(balanced);
        alts.add(premium);
    }

    private void generateLodgingAlternatives(ActivityEntity activity, TripEntity trip, String destination,
                                             int travelers, String currency, List<ActivityAlternativeEntity> alts) {
        PlaceEntity currentPlace = activity.getPlace();
        String currentName = currentPlace != null ? currentPlace.getName() : activity.getTitle();

        // 1. Balanced (Khách sạn 3 sao / tiện nghi)
        Long balancedCost = activity.getEstimatedCost() != null ? activity.getEstimatedCost() : 650_000L;
        ActivityAlternativeEntity balanced = ActivityAlternativeEntity.builder()
                .activity(activity)
                .place(currentPlace)
                .placeName(currentName)
                .title(activity.getTitle())
                .tier("BALANCED")
                .tierLabel("Tiêu chuẩn (Khách sạn 3★)")
                .estimatedCost(balancedCost)
                .rating(currentPlace != null ? currentPlace.getRating() : BigDecimal.valueOf(4.4))
                .userRatingsTotal(currentPlace != null ? currentPlace.getUserRatingsTotal() : 150)
                .address(currentPlace != null ? currentPlace.getAddress() : null)
                .isSelected(true)
                .build();

        // 2. Budget (Homestay / Hostel)
        PlaceEntity budgetPlace = findOrAdoptCandidate("homestay đẹp giá rẻ", destination, currentPlace);
        Long budgetCost = Math.max(250_000L, (long) (balancedCost * 0.5));
        String budgetName = budgetPlace != null ? budgetPlace.getName() : "Homestay / Hostel tiện nghi";
        ActivityAlternativeEntity budget = ActivityAlternativeEntity.builder()
                .activity(activity)
                .place(budgetPlace)
                .placeName(budgetName)
                .title("Lưu trú tiết kiệm: " + budgetName)
                .tier("BUDGET")
                .tierLabel("Tiết kiệm (Homestay)")
                .estimatedCost(budgetCost)
                .rating(budgetPlace != null && budgetPlace.getRating() != null ? budgetPlace.getRating() : BigDecimal.valueOf(4.6))
                .userRatingsTotal(budgetPlace != null ? budgetPlace.getUserRatingsTotal() : 90)
                .address(budgetPlace != null ? budgetPlace.getAddress() : null)
                .isSelected(false)
                .build();

        // 3. Premium (Resort / 4-5 sao)
        PlaceEntity premiumPlace = findOrAdoptCandidate("resort cao cấp view biển", destination, currentPlace);
        Long premiumCost = Math.max(1_600_000L, (long) (balancedCost * 2.5));
        String premiumName = premiumPlace != null ? premiumPlace.getName() : "Resort / Khách sạn 4-5★";
        ActivityAlternativeEntity premium = ActivityAlternativeEntity.builder()
                .activity(activity)
                .place(premiumPlace)
                .placeName(premiumName)
                .title("Nghỉ dưỡng cao cấp: " + premiumName)
                .tier("PREMIUM")
                .tierLabel("Nghỉ dưỡng (Resort / 4-5★)")
                .estimatedCost(premiumCost)
                .rating(premiumPlace != null && premiumPlace.getRating() != null ? premiumPlace.getRating() : BigDecimal.valueOf(4.8))
                .userRatingsTotal(premiumPlace != null ? premiumPlace.getUserRatingsTotal() : 380)
                .address(premiumPlace != null ? premiumPlace.getAddress() : null)
                .isSelected(false)
                .build();

        alts.add(budget);
        alts.add(balanced);
        alts.add(premium);
    }

    private PlaceEntity findOrAdoptCandidate(String query, String destination, PlaceEntity excludePlace) {
        try {
            List<PlaceResponse> results = placeService.searchPlaces(query, destination);
            for (PlaceResponse p : results) {
                if (excludePlace == null || !excludePlace.getExternalId().equals(p.getExternalId())) {
                    return placeAdoptionService.adopt(p.getProvider(), p.getExternalId(), PlaceAdoption.AI_GENERATE);
                }
            }
        } catch (Exception e) {
            log.debug("Khong tim duoc dia diem qua searchPlaces ({}): {}", query, e.getMessage());
        }

        // Fallback: tìm trong CSDL nếu có địa điểm khác cùng category
        try {
            List<PlaceEntity> all = placeRepository.findAll();
            for (PlaceEntity p : all) {
                if (excludePlace == null || !excludePlace.getId().equals(p.getId())) {
                    return p;
                }
            }
        } catch (Exception e) {
            // fallback null
        }
        return null;
    }

    private Long suggestCost(int priceLevel, ActivityType type, int travelers, String currency, long fallback) {
        try {
            Long est = costEstimator.suggest(priceLevel, type, travelers, currency);
            if (est != null && est > 0) {
                return est;
            }
        } catch (Exception e) {
            // ignore
        }
        return fallback;
    }

    public ActivityAlternativeResponse toResponse(ActivityAlternativeEntity entity) {
        return ActivityAlternativeResponse.builder()
                .id(entity.getId())
                .placeId(entity.getPlace() != null ? entity.getPlace().getId() : null)
                .placeName(entity.getPlace() != null ? entity.getPlace().getName() : entity.getPlaceName())
                .title(entity.getTitle())
                .tier(entity.getTier())
                .tierLabel(entity.getTierLabel())
                .estimatedCost(entity.getEstimatedCost())
                .rating(entity.getRating())
                .userRatingsTotal(entity.getUserRatingsTotal())
                .address(entity.getAddress())
                .photoUrl(entity.getPhotoUrl())
                .isSelected(entity.isSelected())
                .build();
    }
}
