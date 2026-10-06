package com.tripmind.services.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tripmind.configurations.properties.AiProperties;
import com.tripmind.domains.proposals.ProposalChange;
import com.tripmind.domains.proposals.ProposalChange.*;
import com.tripmind.domains.responses.ProposalResponse;
import com.tripmind.entities.AiProposalEntity;
import com.tripmind.entities.ActivityEntity;
import com.tripmind.entities.ItineraryDayEntity;
import com.tripmind.entities.TripEntity;
import com.tripmind.enums.ActivityStatus;
import com.tripmind.enums.ActivityType;
import com.tripmind.enums.CostSource;
import com.tripmind.enums.ProposalKind;
import com.tripmind.enums.ProposalStatus;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import com.tripmind.repositories.ActivityRepository;
import com.tripmind.repositories.AiProposalRepository;
import com.tripmind.repositories.ItineraryDayRepository;
import com.tripmind.repositories.TripRepository;
import com.tripmind.services.CostEstimator;
import com.tripmind.services.DistanceService;
import com.tripmind.services.TripClock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Dựng, đọc, từ chối và cho hết hạn đề xuất của trợ lý (FR-701 → FR-710).
 *
 * <p>Lúc dựng, từng thao tác được kiểm với dữ liệu thật: ngày có trong chuyến, hoạt động thuộc
 * chuyến và chưa xong, địa điểm có thật, giờ hợp lệ, danh sách sắp lại khớp đúng tập hoạt động.
 * Thao tác hỏng bị loại kèm lý do (ghi vào {@code evidence_json}) chứ không làm hỏng cả đề xuất.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProposalService {

    private final AiProposalRepository proposalRepository;
    private final TripRepository tripRepository;
    private final ItineraryDayRepository dayRepository;
    private final ActivityRepository activityRepository;
    private final PlaceVerifier placeVerifier;
    private final CostEstimator costEstimator;
    private final DistanceService distanceService;
    private final TripClock tripClock;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    /** Một thao tác ở dạng phẳng mà mô hình điền vào khi gọi {@code propose_itinerary_changes}. */
    public record ChangeInput(
            @ToolParam(description = "ADD, REMOVE, UPDATE or REORDER") String op,
            @ToolParam(description = "ADD: target day. UPDATE: move to this day (optional). REORDER: the day", required = false)
            Integer dayNumber,
            @ToolParam(description = "REMOVE/UPDATE: id from get_itinerary", required = false) Long activityId,
            @ToolParam(description = "ADD (required) / UPDATE: activity title in Vietnamese", required = false) String title,
            @ToolParam(description = "ADD: SIGHTSEEING, FOOD, TRANSPORT, ACCOMMODATION, REST or OTHER", required = false)
            String activityType,
            @ToolParam(description = "ADD: externalId of a place returned by search_places", required = false)
            String placeExternalId,
            @ToolParam(description = "HH:mm", required = false) String startTime,
            @ToolParam(description = "HH:mm", required = false) String endTime,
            @ToolParam(description = "Estimated cost for the whole group in the trip currency", required = false)
            Long estimatedCost,
            @ToolParam(required = false) String notes,
            @ToolParam(description = "REORDER: every activity id of the day in the new order", required = false)
            List<Long> activityIds,
            @ToolParam(description = "Why this change, in Vietnamese") String reason) {
    }

    public record Rejection(int index, String op, String reason) {
    }

    public record CreateResult(Long proposalId, int accepted, List<Rejection> rejected, Long costDelta,
                               Integer travelTimeDelta) {
    }

    @Transactional
    public CreateResult create(AgentTurn turn, String summary, String reason, List<ChangeInput> inputs) {
        TripEntity trip = tripRepository.findByIdAndUserId(turn.getTripId(), turn.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Trip not found or access denied"));
        Map<Integer, ItineraryDayEntity> days = dayRepository.findByTripIdOrderByDayNumberAsc(trip.getId()).stream()
                .collect(Collectors.toMap(d -> (int) d.getDayNumber(), Function.identity()));
        Map<Long, ActivityEntity> activities = activityRepository
                .findByItineraryDayTripIdOrderByItineraryDayDayNumberAscOrderIndexAsc(trip.getId()).stream()
                .collect(Collectors.toMap(ActivityEntity::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));

        List<ProposalChange> changes = new ArrayList<>();
        List<Integer> changeIndexes = new ArrayList<>();
        List<Rejection> rejected = new ArrayList<>();
        Set<Integer> mutatedDays = new HashSet<>();
        List<ChangeInput> safeInputs = inputs == null ? List.of() : inputs;

        for (int i = 0; i < safeInputs.size(); i++) {
            ChangeInput in = safeInputs.get(i);
            String op = in.op() == null ? "" : in.op().trim().toUpperCase();
            try {
                ProposalChange change = switch (op) {
                    case "ADD" -> buildAdd(in, trip, days, turn);
                    case "REMOVE" -> buildRemove(in, activities);
                    case "UPDATE" -> buildUpdate(in, days, activities);
                    case "REORDER" -> buildReorder(in, days, activities);
                    default -> throw new InvalidChange("UNKNOWN_OP");
                };
                switch (change) {
                    case Add a -> mutatedDays.add(a.dayNumber());
                    case Remove r -> mutatedDays.add(r.before().dayNumber());
                    case Update u -> {
                        mutatedDays.add(u.before().dayNumber());
                        if (u.dayNumber() != null) {
                            mutatedDays.add(u.dayNumber());
                        }
                    }
                    case Reorder ignored -> {
                    }
                }
                changes.add(change);
                changeIndexes.add(i);
            } catch (InvalidChange e) {
                rejected.add(new Rejection(i, op, e.getMessage()));
            }
        }

        // Sắp lại một ngày mà cùng đề xuất còn thêm/xoá/chuyển hoạt động của ngày đó thì danh sách
        // sắp lại đã lỗi thời ngay khi áp dụng: loại thao tác sắp lại.
        for (int k = changes.size() - 1; k >= 0; k--) {
            if (changes.get(k) instanceof Reorder r && mutatedDays.contains(r.dayNumber())) {
                rejected.add(new Rejection(changeIndexes.get(k), "REORDER", "CONFLICTS_WITH_OTHER_CHANGES"));
                changes.remove(k);
                changeIndexes.remove(k);
            }
        }
        rejected.sort(Comparator.comparingInt(Rejection::index));

        if (changes.isEmpty()) {
            return new CreateResult(null, 0, rejected, null, null);
        }

        long costDelta = costDelta(changes);
        int travelDelta = travelTimeDelta(changes, days, activities);
        ObjectNode evidence = objectMapper.createObjectNode();
        evidence.put("reason", reason);
        evidence.set("toolExecutionIds", objectMapper.valueToTree(List.copyOf(turn.getExecutionIds())));
        evidence.set("rejected", objectMapper.valueToTree(rejected));
        evidence.put("placesConsidered", turn.getSeenPlaceIds().size());

        AiProposalEntity saved = proposalRepository.save(AiProposalEntity.builder()
                .tripId(trip.getId())
                .conversationId(turn.getConversationId())
                .summary(summary == null || summary.isBlank() ? "Đề xuất thay đổi lịch trình" : summary.strip())
                .changes(ProposalChange.toJson(objectMapper, changes))
                .estimatedCostDelta(costDelta)
                .travelTimeDelta(travelDelta)
                .status(ProposalStatus.PENDING)
                .kind(ProposalKind.ITINERARY)
                .evidence(evidence)
                .expiresAt(tripClock.now().plus(properties.proposalTtl()))
                .build());

        turn.getProposalIds().add(saved.getId());
        turn.emit("proposal", Map.of("proposalId", saved.getId(), "summary", saved.getSummary(),
                "estimatedCostDelta", costDelta, "travelTimeDelta", travelDelta, "changes", changes.size()));
        log.info("Proposal ID={} for trip ID={}: {} changes, {} rejected", saved.getId(), trip.getId(), changes.size(),
                rejected.size());
        return new CreateResult(saved.getId(), changes.size(), rejected, costDelta, travelDelta);
    }

    @Transactional(readOnly = true)
    public ProposalResponse get(Long userId, Long proposalId) {
        return toResponse(owned(userId, proposalId, null));
    }

    @Transactional
    public ProposalResponse reject(Long userId, Long proposalId) {
        AiProposalEntity proposal = owned(userId, proposalId, null);
        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new AppException(ErrorCode.PROPOSAL_NOT_PENDING, "Proposal is " + proposal.getStatus());
        }
        proposal.setStatus(ProposalStatus.REJECTED);
        return toResponse(proposalRepository.save(proposal));
    }

    /** Đề xuất chờ duyệt quá hạn chuyển sang EXPIRED (FR-707). */
    @Scheduled(fixedDelayString = "${tripmind.ai.expiry-check-interval:PT1M}")
    @Transactional
    public void expirePending() {
        int expired = proposalRepository.expirePending(tripClock.now());
        if (expired > 0) {
            log.info("Expired {} pending proposals", expired);
        }
    }

    /** Đề xuất thuộc chuyến của người gọi (và đúng chuyến trong đường dẫn nếu có), ngược lại 404. */
    AiProposalEntity owned(Long userId, Long proposalId, Long tripId) {
        AiProposalEntity proposal = proposalRepository.findById(proposalId)
                .filter(p -> tripId == null || p.getTripId().equals(tripId))
                .orElseThrow(() -> new AppException(ErrorCode.PROPOSAL_NOT_FOUND, "Proposal not found: " + proposalId));
        tripRepository.findByIdAndUserId(proposal.getTripId(), userId)
                .orElseThrow(() -> new AppException(ErrorCode.PROPOSAL_NOT_FOUND, "Proposal not found: " + proposalId));
        return proposal;
    }

    public List<ProposalChange> changesOf(AiProposalEntity proposal) {
        return objectMapper.convertValue(proposal.getChanges(), new TypeReference<>() {
        });
    }

    ProposalResponse toResponse(AiProposalEntity p) {
        JsonNode evidence = p.getEvidence();
        return ProposalResponse.builder()
                .id(p.getId())
                .tripId(p.getTripId())
                .conversationId(p.getConversationId())
                .kind(p.getKind())
                .status(p.getStatus())
                .summary(p.getSummary())
                .reason(evidence == null || evidence.get("reason") == null ? null : evidence.get("reason").asText())
                .changes(p.getChanges())
                .rejected(evidence == null ? null : evidence.get("rejected"))
                .estimatedCostDelta(p.getEstimatedCostDelta())
                .travelTimeDelta(p.getTravelTimeDelta())
                .expiresAt(p.getExpiresAt())
                .appliedAt(p.getAppliedAt())
                .revertedAt(p.getRevertedAt())
                .undoAvailableUntil(p.getStatus() == ProposalStatus.APPLIED && p.getAppliedAt() != null
                        ? p.getAppliedAt().plus(properties.undoWindow()) : null)
                .build();
    }

    private Add buildAdd(ChangeInput in, TripEntity trip, Map<Integer, ItineraryDayEntity> days, AgentTurn turn) {
        if (in.dayNumber() == null || !days.containsKey(in.dayNumber())) {
            throw new InvalidChange("DAY_NOT_FOUND");
        }
        if (in.title() == null || in.title().isBlank() || in.title().length() > 200) {
            throw new InvalidChange("INVALID_TITLE");
        }
        ActivityType type = parseType(in.activityType());
        PlaceRef place = null;
        if (in.placeExternalId() != null && !in.placeExternalId().isBlank()) {
            place = placeVerifier.verify(in.placeExternalId(), turn)
                    .orElseThrow(() -> new InvalidChange("PLACE_NOT_VERIFIED"));
        }
        LocalTime start = parseTime(in.startTime());
        LocalTime end = parseTime(in.endTime());
        checkRange(start, end);
        Long cost = in.estimatedCost();
        CostSource source = cost == null ? null : CostSource.AI;
        if (cost == null && place != null) {
            cost = costEstimator.suggest(place.priceLevel(), type, trip.getTravelers(), trip.getCurrency());
            source = cost == null ? null : CostSource.PRICE_LEVEL;
        }
        if (cost != null && cost < 0) {
            throw new InvalidChange("NEGATIVE_COST");
        }
        return new Add(in.dayNumber(), in.title().strip(), type, place, start, end, cost, source, in.notes(), in.reason());
    }

    private Remove buildRemove(ChangeInput in, Map<Long, ActivityEntity> activities) {
        ActivityEntity activity = requireOpenActivity(in.activityId(), activities);
        return new Remove(activity.getId(), snapshot(activity), in.reason());
    }

    private Update buildUpdate(ChangeInput in, Map<Integer, ItineraryDayEntity> days, Map<Long, ActivityEntity> activities) {
        ActivityEntity activity = requireOpenActivity(in.activityId(), activities);
        Integer targetDay = in.dayNumber();
        if (targetDay != null && !days.containsKey(targetDay)) {
            throw new InvalidChange("DAY_NOT_FOUND");
        }
        if (targetDay != null && targetDay == activity.getItineraryDay().getDayNumber()) {
            targetDay = null;
        }
        LocalTime start = parseTime(in.startTime());
        LocalTime end = parseTime(in.endTime());
        checkRange(start != null ? start : activity.getStartTime(), end != null ? end : activity.getEndTime());
        if (in.estimatedCost() != null && in.estimatedCost() < 0) {
            throw new InvalidChange("NEGATIVE_COST");
        }
        String title = in.title() == null || in.title().isBlank() ? null : in.title().strip();
        if (targetDay == null && title == null && start == null && end == null && in.estimatedCost() == null
                && in.notes() == null) {
            throw new InvalidChange("NOTHING_TO_UPDATE");
        }
        return new Update(activity.getId(), targetDay, title, start, end, in.estimatedCost(), in.notes(),
                snapshot(activity), in.reason());
    }

    private Reorder buildReorder(ChangeInput in, Map<Integer, ItineraryDayEntity> days, Map<Long, ActivityEntity> activities) {
        if (in.dayNumber() == null || !days.containsKey(in.dayNumber())) {
            throw new InvalidChange("DAY_NOT_FOUND");
        }
        List<Long> current = activities.values().stream()
                .filter(a -> a.getItineraryDay().getDayNumber() == in.dayNumber())
                .map(ActivityEntity::getId)
                .toList();
        List<Long> requested = in.activityIds() == null ? List.of() : in.activityIds();
        if (requested.size() != current.size() || !new HashSet<>(requested).equals(new HashSet<>(current))) {
            throw new InvalidChange("REORDER_SET_MISMATCH");
        }
        if (requested.equals(current)) {
            throw new InvalidChange("ORDER_UNCHANGED");
        }
        return new Reorder(in.dayNumber(), List.copyOf(requested), current, in.reason());
    }

    /** FR-1009: không đề xuất sửa hoạt động đã xong hoặc đã bỏ. */
    private ActivityEntity requireOpenActivity(Long id, Map<Long, ActivityEntity> activities) {
        ActivityEntity activity = id == null ? null : activities.get(id);
        if (activity == null) {
            throw new InvalidChange("ACTIVITY_NOT_FOUND");
        }
        if (activity.getStatus() == ActivityStatus.DONE || activity.getStatus() == ActivityStatus.SKIPPED) {
            throw new InvalidChange("ACTIVITY_FINISHED");
        }
        return activity;
    }

    static ActivitySnapshot snapshot(ActivityEntity a) {
        return new ActivitySnapshot(a.getId(), (int) a.getItineraryDay().getDayNumber(), a.getTitle(), a.getActivityType(),
                a.getStartTime(), a.getEndTime(), a.getEstimatedCost(), a.getNotes(),
                a.getPlace() == null ? null : a.getPlace().getName());
    }

    private long costDelta(List<ProposalChange> changes) {
        long delta = 0;
        for (ProposalChange change : changes) {
            switch (change) {
                case Add a -> delta += nz(a.estimatedCost());
                case Remove r -> delta -= nz(r.before().estimatedCost());
                case Update u -> {
                    if (u.estimatedCost() != null) {
                        delta += u.estimatedCost() - nz(u.before().estimatedCost());
                    }
                }
                case Reorder ignored -> {
                }
            }
        }
        return delta;
    }

    /** Mô phỏng thứ tự các ngày bị ảnh hưởng trước và sau đề xuất, cộng chênh lệch phút di chuyển. */
    private int travelTimeDelta(List<ProposalChange> changes, Map<Integer, ItineraryDayEntity> days,
                                Map<Long, ActivityEntity> activities) {
        Map<Integer, List<Node>> before = new HashMap<>();
        for (ActivityEntity a : activities.values()) {
            before.computeIfAbsent((int) a.getItineraryDay().getDayNumber(), d -> new ArrayList<>()).add(Node.of(a));
        }
        Map<Integer, List<Node>> after = new HashMap<>();
        before.forEach((day, nodes) -> after.put(day, new ArrayList<>(nodes)));
        Set<Integer> affected = new HashSet<>();
        int newKey = -1;
        for (ProposalChange change : changes) {
            switch (change) {
                case Add a -> {
                    affected.add(a.dayNumber());
                    after.computeIfAbsent(a.dayNumber(), d -> new ArrayList<>()).add(new Node(newKey--,
                            a.place() == null ? null : a.place().lat(), a.place() == null ? null : a.place().lng(), null));
                }
                case Remove r -> {
                    affected.add(r.before().dayNumber());
                    after.getOrDefault(r.before().dayNumber(), new ArrayList<>()).removeIf(n -> n.id() == r.activityId());
                }
                case Update u -> {
                    if (u.dayNumber() != null) {
                        List<Node> source = after.getOrDefault(u.before().dayNumber(), new ArrayList<>());
                        Node moved = source.stream().filter(n -> n.id() == u.activityId()).findFirst().orElse(null);
                        source.remove(moved);
                        if (moved != null) {
                            after.computeIfAbsent(u.dayNumber(), d -> new ArrayList<>()).add(moved);
                        }
                        affected.add(u.before().dayNumber());
                        affected.add(u.dayNumber());
                    }
                }
                case Reorder r -> {
                    affected.add(r.dayNumber());
                    Map<Long, Node> byId = after.getOrDefault(r.dayNumber(), List.of()).stream()
                            .collect(Collectors.toMap(Node::id, Function.identity()));
                    after.put(r.dayNumber(), r.activityIds().stream().map(byId::get).filter(Objects::nonNull)
                            .collect(Collectors.toCollection(ArrayList::new)));
                }
            }
        }
        int delta = 0;
        for (Integer day : affected) {
            delta += minutes(after.getOrDefault(day, List.of())) - minutes(before.getOrDefault(day, List.of()));
        }
        return delta;
    }

    private int minutes(List<Node> nodes) {
        int total = 0;
        for (int i = 0; i + 1 < nodes.size(); i++) {
            Node a = nodes.get(i);
            Node b = nodes.get(i + 1);
            if (a.lat() == null || b.lat() == null) {
                continue;
            }
            long meters = distanceService.calculateDistanceMeters(a.lat().doubleValue(), a.lng().doubleValue(),
                    b.lat().doubleValue(), b.lng().doubleValue());
            total += distanceService.estimateTravelMinutes(meters, b.mode());
        }
        return total;
    }

    private record Node(long id, BigDecimal lat, BigDecimal lng, String mode) {
        static Node of(ActivityEntity a) {
            return new Node(a.getId(), a.getPlace() == null ? null : a.getPlace().getLatitude(),
                    a.getPlace() == null ? null : a.getPlace().getLongitude(), a.getTransportationMode());
        }
    }

    private ActivityType parseType(String value) {
        if (value == null || value.isBlank()) {
            return ActivityType.OTHER;
        }
        try {
            return ActivityType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidChange("INVALID_ACTIVITY_TYPE");
        }
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidChange("INVALID_TIME");
        }
    }

    private void checkRange(LocalTime start, LocalTime end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new InvalidChange("INVALID_TIME_RANGE");
        }
    }

    private static long nz(Long value) {
        return value == null ? 0 : value;
    }

    /** Một thao tác không hợp lệ; thông điệp là mã lý do trả lại cho mô hình. */
    static class InvalidChange extends RuntimeException {
        InvalidChange(String reason) {
            super(reason, null, false, false);
        }
    }
}
