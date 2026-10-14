package com.tripmind.services;

import com.tripmind.domains.proposals.ProposalChange.PlaceRef;
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
import com.tripmind.services.clients.GooglePlacesClient.GooglePlace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;

/**
 * Ba lựa chọn theo ngân sách cho hoạt động ăn uống / lưu trú. Chỉ dùng địa điểm có thật (tìm
 * được), không bịa tên, điểm đánh giá hay giá: thiếu dữ liệu thì để trống. Tìm kiếm (HTTP) chạy
 * ngoài giao dịch; chỉ phần ghi nằm trong giao dịch.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityAlternativeService {

    private static final String BALANCED = "BALANCED";

    private record Tier(String code, String label, String query, String titlePrefix, int priceLevel, double lodgingFactor) {
    }

    private static final List<Tier> FOOD_TIERS = List.of(
            new Tier("BUDGET", "Tiết kiệm (Bình dân)", "quán ăn bình dân ngon", "Ẩm thực bình dân: ", 1, 0),
            new Tier("PREMIUM", "Trải nghiệm (Cao cấp)", "nhà hàng cao cấp view đẹp", "Trải nghiệm ẩm thực: ", 3, 0));
    private static final List<Tier> LODGING_TIERS = List.of(
            new Tier("BUDGET", "Tiết kiệm (Homestay)", "homestay đẹp giá rẻ", "Lưu trú tiết kiệm: ", 1, 0.5),
            new Tier("PREMIUM", "Nghỉ dưỡng (Resort / 4-5★)", "resort cao cấp", "Nghỉ dưỡng cao cấp: ", 3, 2.5));

    private final ActivityRepository activityRepository;
    private final ActivityAlternativeRepository alternativeRepository;
    private final PlaceService placeService;
    private final PlaceAdoptionService placeAdoptionService;
    private final CostEstimator costEstimator;
    private final TransactionTemplate transactionTemplate;

    public List<ActivityAlternativeResponse> getOrGenerateAlternatives(Long userId, Long activityId) {
        activityRepository.findByIdAndUserId(activityId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Activity not found or access denied: " + activityId));
        return generateForActivity(activityId).stream().map(this::toResponse).toList();
    }

    /** Sinh (hoặc trả lại nếu đã có) lựa chọn thay thế; dùng được từ luồng nền, không cần giao dịch bên ngoài. */
    public List<ActivityAlternativeEntity> generateForActivity(Long activityId) {
        List<ActivityAlternativeEntity> existing = alternativeRepository.findByActivityIdWithPlace(activityId);
        if (!existing.isEmpty()) {
            return existing;
        }
        ActivityEntity activity = activityRepository.findDetachedById(activityId).orElse(null);
        if (activity == null || (activity.getActivityType() != ActivityType.FOOD
                && activity.getActivityType() != ActivityType.ACCOMMODATION)) {
            return List.of();
        }
        TripEntity trip = activity.getItineraryDay().getTrip();
        List<Tier> tiers = activity.getActivityType() == ActivityType.FOOD ? FOOD_TIERS : LODGING_TIERS;
        Map<Tier, PlaceResponse> candidates = findCandidates(activity, trip, tiers);
        Map<String, GooglePlace> prefetched = placeAdoptionService.prefetch(candidates.values().stream()
                .map(p -> new PlaceRef(p.getProvider(), p.getExternalId(), p.getName(), p.getLatitude(), p.getLongitude(),
                        p.getPriceLevel()))
                .toList());
        return transactionTemplate.execute(status -> {
            List<ActivityAlternativeEntity> again = alternativeRepository.findByActivityIdWithPlace(activityId);
            return again.isEmpty() ? alternativeRepository.saveAll(build(activity, trip, tiers, candidates, prefetched)) : again;
        });
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

    private Map<Tier, PlaceResponse> findCandidates(ActivityEntity activity, TripEntity trip, List<Tier> tiers) {
        Map<Tier, PlaceResponse> found = new LinkedHashMap<>();
        String destination = trip.getDestination() == null ? null : trip.getDestination().getName();
        if (destination == null || destination.isBlank()) {
            return found;
        }
        Set<String> used = new HashSet<>();
        if (activity.getPlace() != null) {
            used.add(activity.getPlace().getExternalId());
        }
        for (Tier tier : tiers) {
            try {
                placeService.searchPlaces(tier.query(), destination).stream()
                        .filter(p -> p.getExternalId() != null && !used.contains(p.getExternalId()))
                        .findFirst()
                        .ifPresent(p -> {
                            used.add(p.getExternalId());
                            found.put(tier, p);
                        });
            } catch (RuntimeException e) {
                log.debug("Khong tim duoc dia diem cho lua chon {} ({}): {}", tier.code(), tier.query(), e.getMessage());
            }
        }
        return found;
    }

    private List<ActivityAlternativeEntity> build(ActivityEntity activity, TripEntity trip, List<Tier> tiers,
                                                  Map<Tier, PlaceResponse> candidates, Map<String, GooglePlace> prefetched) {
        boolean food = activity.getActivityType() == ActivityType.FOOD;
        int travelers = Math.max(1, trip.getTravelers());
        String currency = trip.getCurrency();
        PlaceEntity current = activity.getPlace();
        ActivityEntity ref = activityRepository.getReferenceById(activity.getId());

        Long balancedCost = activity.getEstimatedCost() != null ? activity.getEstimatedCost()
                : food ? costEstimator.suggest(current == null || current.getPriceLevel() == null ? 2
                : current.getPriceLevel().intValue(), ActivityType.FOOD, travelers, currency) : null;
        List<ActivityAlternativeEntity> alts = new ArrayList<>();
        alts.add(ActivityAlternativeEntity.builder()
                .activity(ref)
                .place(current)
                .placeName(current != null ? current.getName() : activity.getTitle())
                .title(activity.getTitle())
                .tier(BALANCED)
                .tierLabel("Tiêu chuẩn (Đề xuất)")
                .estimatedCost(balancedCost)
                .rating(current != null ? current.getRating() : null)
                .userRatingsTotal(current != null ? current.getUserRatingsTotal() : null)
                .address(current != null ? current.getAddress() : null)
                .isSelected(true)
                .build());

        for (Tier tier : tiers) {
            PlaceResponse candidate = candidates.get(tier);
            if (candidate == null) {
                continue;
            }
            PlaceRef placeRef = new PlaceRef(candidate.getProvider(), candidate.getExternalId(), candidate.getName(),
                    candidate.getLatitude(), candidate.getLongitude(), candidate.getPriceLevel());
            Optional<PlaceEntity> place = placeAdoptionService.adoptPrefetched(placeRef, prefetched, PlaceAdoption.AI_GENERATE);
            if (place.isEmpty()) {
                continue;
            }
            PlaceEntity p = place.get();
            Long cost = food
                    ? costEstimator.suggest(p.getPriceLevel() != null ? p.getPriceLevel().intValue() : tier.priceLevel(),
                    ActivityType.FOOD, travelers, currency)
                    : balancedCost == null ? null : Math.round(balancedCost * tier.lodgingFactor());
            alts.add(ActivityAlternativeEntity.builder()
                    .activity(ref)
                    .place(p)
                    .placeName(p.getName())
                    .title(tier.titlePrefix() + p.getName())
                    .tier(tier.code())
                    .tierLabel(tier.label())
                    .estimatedCost(cost)
                    .rating(p.getRating())
                    .userRatingsTotal(p.getUserRatingsTotal())
                    .address(p.getAddress())
                    .isSelected(false)
                    .build());
        }
        alts.sort(Comparator.comparingInt(a -> List.of("BUDGET", BALANCED, "PREMIUM").indexOf(a.getTier())));
        return alts;
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
