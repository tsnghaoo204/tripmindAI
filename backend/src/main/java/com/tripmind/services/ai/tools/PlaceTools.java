package com.tripmind.services.ai.tools;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripmind.configurations.properties.AiProperties;
import com.tripmind.domains.models.GroupProfile;
import com.tripmind.domains.responses.PlaceResponse;
import com.tripmind.entities.TripEntity;
import com.tripmind.enums.DietaryRestriction;
import com.tripmind.repositories.PlaceRepository;
import com.tripmind.services.DistanceService;
import com.tripmind.services.PlaceService;
import com.tripmind.services.TripService;
import com.tripmind.services.ai.AgentToolSet;
import com.tripmind.services.ai.AgentTurn;
import com.tripmind.services.ai.PlaceVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/** Công cụ địa điểm: tìm kiếm, tính quãng đường, và đưa ứng viên cho người dùng chọn. */
@Component
@RequiredArgsConstructor
public class PlaceTools implements AgentToolSet {

    /** Từ khoá cho biết người dùng đang tìm chỗ ăn uống. */
    private static final List<String> FOOD_WORDS = List.of(
            "ăn", "quán", "nhà hàng", "món", "food", "restaurant", "cafe", "cà phê", "hải sản", "bún", "phở");
    private static final long LONG_WALK_METERS = 1000;

    private final PlaceService placeService;
    private final PlaceRepository placeRepository;
    private final PlaceVerifier placeVerifier;
    private final TripService tripService;
    private final DistanceService distanceService;
    private final PlaceRatingLookup placeRatingLookup;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    @Tool(name = "search_places", description = """
            Search real places near the trip destination (restaurants, attractions, cafes...).
            Returns at most 5 places with their externalId. Only places returned here may be proposed.""")
    public String searchPlaces(
            @ToolParam(description = "What to look for, e.g. 'hải sản gần biển Mỹ Khê' or 'bảo tàng'") String query,
            ToolContext context) {
        AgentTurn turn = ToolSupport.turn(context);
        TripEntity trip = tripService.getOwnedTrip(turn.getUserId(), turn.getTripId());
        String effectiveQuery = withDietaryKeywords(query, trip.getGroupProfile());

        Set<String> disliked = placeRatingLookup.dislikedExternalIds(turn.getUserId());
        Set<String> liked = placeRatingLookup.likedExternalIds(turn.getUserId());
        List<PlaceView> results = new ArrayList<>();
        int skippedDisliked = 0;
        for (PlaceResponse place : placeService.searchPlaces(effectiveQuery, trip.getDestination().getName())) {
            if (place.getExternalId() != null && disliked.contains(place.getExternalId())) {
                skippedDisliked++;
                continue;
            }
            PlaceView view = PlaceView.of(place);
            results.add(liked.contains(place.getExternalId()) ? view.liked() : view);
            turn.getSeenPlaceIds().add(place.getExternalId());
            if (results.size() >= properties.maxSearchResults()) {
                break;
            }
        }
        return ToolSupport.json(objectMapper, new SearchResult(effectiveQuery, results,
                skippedDisliked == 0 ? null : skippedDisliked, results.isEmpty() ? "NO_RESULTS" : null));
    }

    @Tool(name = "calculate_distance", description = """
            Straight-line distance and estimated travel time between two coordinates.
            Mode: WALKING, BICYCLE, MOTORBIKE (default), CAR.""")
    public String calculateDistance(
            @ToolParam(description = "Origin latitude") BigDecimal fromLat,
            @ToolParam(description = "Origin longitude") BigDecimal fromLng,
            @ToolParam(description = "Destination latitude") BigDecimal toLat,
            @ToolParam(description = "Destination longitude") BigDecimal toLng,
            @ToolParam(description = "Transportation mode", required = false) String mode,
            ToolContext context) {
        AgentTurn turn = ToolSupport.turn(context);
        long meters = distanceService.calculateDistanceMeters(fromLat.doubleValue(), fromLng.doubleValue(),
                toLat.doubleValue(), toLng.doubleValue());
        String effectiveMode = mode == null || mode.isBlank() ? "MOTORBIKE" : mode.trim().toUpperCase();
        int minutes = distanceService.estimateTravelMinutes(meters, effectiveMode);
        GroupProfile group = tripService.getOwnedTrip(turn.getUserId(), turn.getTripId()).getGroupProfile();
        boolean longWalk = effectiveMode.startsWith("WALK") && meters > LONG_WALK_METERS
                && group != null && group.needsGentlePace();
        return ToolSupport.json(objectMapper, new DistanceResult(meters, distanceService.formatDistance(meters),
                minutes, distanceService.formatDuration(minutes), effectiveMode, longWalk ? "LONG_WALK" : null));
    }

    @Tool(name = "propose_places", description = """
            Show place suggestions to the user as cards they can pick from. Each candidate must be a place
            returned by search_places or get_saved_places in this conversation turn. Nothing is saved until the
            user picks one.""")
    public String proposePlaces(
            @ToolParam(description = "Candidates with the externalId from search_places and a short reason in Vietnamese")
            List<Candidate> candidates,
            ToolContext context) {
        AgentTurn turn = ToolSupport.turn(context);
        List<Map<String, Object>> accepted = new ArrayList<>();
        List<Map<String, String>> rejected = new ArrayList<>();
        for (Candidate candidate : candidates == null ? List.<Candidate>of() : candidates) {
            Optional<PlaceView> place = verified(candidate.externalId(), turn);
            if (place.isEmpty()) {
                rejected.add(Map.of("externalId", String.valueOf(candidate.externalId()), "reason", "PLACE_NOT_VERIFIED"));
                continue;
            }
            Map<String, Object> card = new LinkedHashMap<>(objectMapper.convertValue(place.get(), Map.class));
            card.put("reason", candidate.reason());
            accepted.add(card);
        }
        if (!accepted.isEmpty()) {
            turn.getAttachments().put("places", objectMapper.valueToTree(accepted));
            turn.emit("places", Map.of("candidates", accepted));
        }
        return ToolSupport.json(objectMapper, Map.of("shown", accepted.size(), "rejected", rejected));
    }

    private Optional<PlaceView> verified(String externalId, AgentTurn turn) {
        return placeVerifier.verify(externalId, turn)
                .map(ref -> placeRepository.findFirstByExternalId(ref.externalId())
                        .map(p -> PlaceView.of(PlaceResponse.fromEntity(p)))
                        .orElseGet(() -> new PlaceView(ref.provider(), ref.externalId(), ref.name(), null, null,
                                ref.priceLevel(), null, ref.lat(), ref.lng(), null)));
    }

    /** Nhóm có người ăn chay / halal: thêm từ khoá vào truy vấn tìm quán ăn — bằng mã, không nhờ mô hình nhớ. */
    public static String withDietaryKeywords(String query, GroupProfile group) {
        if (group == null || group.dietaryOrEmpty().isEmpty() || query == null) {
            return query;
        }
        String lower = query.toLowerCase(Locale.ROOT);
        if (FOOD_WORDS.stream().noneMatch(lower::contains)) {
            return query;
        }
        StringBuilder result = new StringBuilder(query);
        Set<DietaryRestriction> diets = group.dietaryOrEmpty();
        if ((diets.contains(DietaryRestriction.VEGETARIAN) || diets.contains(DietaryRestriction.VEGAN)) && !lower.contains("chay")) {
            result.append(" chay");
        }
        if (diets.contains(DietaryRestriction.HALAL) && !lower.contains("halal")) {
            result.append(" halal");
        }
        return result.toString();
    }

    public record Candidate(
            @ToolParam(description = "externalId returned by search_places") String externalId,
            @ToolParam(description = "Why this place fits, in Vietnamese") String reason) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record SearchResult(String query, List<PlaceView> places, Integer hiddenDislikedPlaces, String reason) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record DistanceResult(long meters, String distance, int minutes, String duration, String mode, String warning) {
    }
}
