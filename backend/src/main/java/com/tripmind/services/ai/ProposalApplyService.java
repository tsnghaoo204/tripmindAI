package com.tripmind.services.ai;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripmind.configurations.properties.AiProperties;
import com.tripmind.domains.proposals.ProposalChange;
import com.tripmind.domains.proposals.ProposalChange.*;
import com.tripmind.domains.responses.ApplyProposalResponse;
import com.tripmind.domains.responses.UndoProposalResponse;
import com.tripmind.entities.*;
import com.tripmind.enums.*;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import com.tripmind.repositories.*;
import com.tripmind.services.ItineraryService;
import com.tripmind.services.PlaceAdoptionService;
import com.tripmind.services.TripClock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.util.*;

/**
 * Áp dụng và hoàn tác đề xuất.
 *
 * <p><b>Áp dụng</b> (FR-705 → FR-709) chỉ nhận mã đề xuất (BR-506), kiểm bốn điều, rồi chạy
 * mọi thao tác trong một giao dịch. Từng thao tác được kiểm lại với dữ liệu hiện tại vì lịch
 * trình có thể đã đổi từ lúc dựng: thao tác trỏ tới hoạt động đã biến mất thì bỏ qua và báo
 * trong {@code skipped}; danh sách sắp lại lỗi thời thì huỷ cả đề xuất.
 *
 * <p><b>Hoàn tác</b> (FR-1201 → FR-1207) không phải phép nghịch đảo toán học: lúc áp dụng ta ghi
 * "dấu vân tay" từng hoạt động; lúc lùi, hoạt động nào có vân tay lệch (người dùng đã sửa tay)
 * thì giữ nguyên và báo trong {@code undoSkipped}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProposalApplyService {

    private final AiProposalRepository proposalRepository;
    private final TripRepository tripRepository;
    private final ItineraryDayRepository dayRepository;
    private final ActivityRepository activityRepository;
    private final PlaceRepository placeRepository;
    private final PlaceAdoptionService placeAdoptionService;
    private final ProposalService proposalService;
    private final ItineraryService itineraryService;
    private final TripClock tripClock;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    // ---------------------------------------------------------------- undo log

    /** Ảnh chụp đầy đủ một hoạt động, đủ để dựng lại nguyên trạng. */
    public record FullSnapshot(long dayId, short orderIndex, String title, ActivityType activityType, Long placeId,
                               LocalTime startTime, LocalTime endTime, Long estimatedCost, CostSource costSource,
                               String transportationMode, String notes, ActivityStatus status, ActivityCreator createdBy,
                               Long fromProposalId) {
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "op")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = UndoOp.Created.class, name = "CREATED"),
            @JsonSubTypes.Type(value = UndoOp.Removed.class, name = "REMOVED"),
            @JsonSubTypes.Type(value = UndoOp.Updated.class, name = "UPDATED"),
            @JsonSubTypes.Type(value = UndoOp.Reordered.class, name = "REORDERED")
    })
    public sealed interface UndoOp {
        int changeIndex();

        record Created(int changeIndex, long activityId, String title, String fingerprint) implements UndoOp {
        }

        record Removed(int changeIndex, FullSnapshot snapshot) implements UndoOp {
        }

        record Updated(int changeIndex, long activityId, FullSnapshot before, String fingerprint) implements UndoOp {
        }

        record Reordered(int changeIndex, long dayId, int dayNumber, List<Long> beforeOrder, List<Long> afterOrder)
                implements UndoOp {
        }
    }

    public record UndoLog(List<UndoOp> ops, Set<Long> touchedActivityIds, List<String> adoptedPlaces) {
    }

    // ---------------------------------------------------------------- apply

    @Transactional
    public ApplyProposalResponse apply(Long userId, Long tripId, Long proposalId) {
        return apply(userId, tripId, proposalId, PlaceAdoption.PROPOSAL);
    }

    /** {@code via} ghi vào {@code places.adopted_via} cho địa điểm mới: PROPOSAL, hoặc AI_GENERATE khi sinh lịch trình. */
    @Transactional
    public ApplyProposalResponse apply(Long userId, Long tripId, Long proposalId, PlaceAdoption via) {
        AiProposalEntity proposal = proposalRepository.findByIdForUpdate(proposalId)
                .filter(p -> p.getTripId().equals(tripId))
                .orElseThrow(() -> new AppException(ErrorCode.PROPOSAL_NOT_FOUND, "Proposal not found: " + proposalId));
        TripEntity trip = tripRepository.findByIdAndUserId(tripId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.PROPOSAL_NOT_FOUND, "Proposal not found: " + proposalId));
        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new AppException(ErrorCode.PROPOSAL_NOT_PENDING, "Proposal is " + proposal.getStatus());
        }
        if (proposal.getExpiresAt().isBefore(tripClock.now())) {
            throw new AppException(ErrorCode.PROPOSAL_EXPIRED, "Proposal expired at " + proposal.getExpiresAt());
        }

        List<ProposalChange> changes = proposalService.changesOf(proposal);
        Map<Integer, ItineraryDayEntity> days = daysByNumber(trip.getId());
        checkReordersStillValid(changes, days);

        List<UndoOp> undo = new ArrayList<>();
        List<ApplyProposalResponse.Skipped> skipped = new ArrayList<>();
        Set<Long> touchedDays = new HashSet<>();
        List<String> adoptedPlaces = new ArrayList<>();
        int applied = 0;

        for (int i = 0; i < changes.size(); i++) {
            ProposalChange change = changes.get(i);
            String skipReason = switch (change) {
                case Add a -> applyAdd(i, a, proposal, days, undo, touchedDays, adoptedPlaces, via);
                case Remove r -> applyRemove(i, r, trip, undo, touchedDays);
                case Update u -> applyUpdate(i, u, trip, days, undo, touchedDays);
                case Reorder r -> applyReorder(i, r, days, undo, touchedDays);
            };
            if (skipReason == null) {
                applied++;
            } else {
                skipped.add(skipped(change, skipReason));
            }
        }
        activityRepository.flush();
        touchedDays.forEach(this::reindex);
        activityRepository.flush();

        List<UndoOp> fingerprinted = undo.stream().map(this::withFingerprint).toList();
        Set<Long> touchedActivities = new HashSet<>();
        for (UndoOp op : fingerprinted) {
            touchedActivities.addAll(activityIdsOf(op));
        }

        proposal.setStatus(ProposalStatus.APPLIED);
        proposal.setAppliedAt(tripClock.now());
        proposal.setAppliedSeq(proposalRepository.maxAppliedSeq(tripId) + 1);
        proposal.setUndo(objectMapper.valueToTree(new UndoLog(fingerprinted, touchedActivities, adoptedPlaces)));
        proposalRepository.save(proposal);
        log.info("Applied proposal ID={} on trip ID={}: {} applied, {} skipped", proposalId, tripId, applied, skipped.size());

        return ApplyProposalResponse.builder()
                .proposalId(proposalId)
                .applied(applied)
                .skipped(skipped)
                .undoAvailableUntil(proposal.getAppliedAt().plus(properties.undoWindow()))
                .itinerary(itineraryService.getItinerary(userId, tripId))
                .build();
    }

    /** Danh sách sắp lại không còn khớp tập hoạt động của ngày thì huỷ cả đề xuất (ADS-21 §4.4). */
    private void checkReordersStillValid(List<ProposalChange> changes, Map<Integer, ItineraryDayEntity> days) {
        for (ProposalChange change : changes) {
            if (change instanceof Reorder r) {
                ItineraryDayEntity day = days.get(r.dayNumber());
                Set<Long> current = day == null ? Set.of() : new HashSet<>(idsOfDay(day.getId()));
                if (!current.equals(new HashSet<>(r.activityIds()))) {
                    throw new AppException(ErrorCode.PROPOSAL_STALE,
                            "Day " + r.dayNumber() + " changed since the proposal was created",
                            Map.of("dayNumber", r.dayNumber()));
                }
            }
        }
    }

    private String applyAdd(int index, Add a, AiProposalEntity proposal, Map<Integer, ItineraryDayEntity> days,
                            List<UndoOp> undo, Set<Long> touchedDays, List<String> adoptedPlaces, PlaceAdoption via) {
        ItineraryDayEntity day = days.get(a.dayNumber());
        if (day == null) {
            return "DAY_NOT_FOUND";
        }
        PlaceEntity place = null;
        if (a.place() != null) {
            boolean existed = placeRepository.findByProviderAndExternalId(a.place().provider(), a.place().externalId()).isPresent();
            try {
                place = placeAdoptionService.adopt(a.place().provider(), a.place().externalId(), via);
            } catch (AppException e) {
                return "PLACE_UNAVAILABLE";
            }
            if (!existed) {
                adoptedPlaces.add(place.getName());
            }
        }
        ActivityEntity activity = activityRepository.save(ActivityEntity.builder()
                .itineraryDay(day)
                .title(a.title())
                .place(place)
                .activityType(a.activityType())
                .createdBy(ActivityCreator.AI)
                .startTime(a.startTime())
                .endTime(a.endTime())
                .estimatedCost(a.estimatedCost())
                .estimatedCostSource(a.costSource())
                .transportationMode("MOTORBIKE")
                .notes(a.notes())
                .orderIndex(nextOrder(day.getId()))
                .status(ActivityStatus.PLANNED)
                .fromProposalId(proposal.getId())
                .build());
        activityRepository.flush();
        undo.add(new UndoOp.Created(index, activity.getId(), activity.getTitle(), null));
        touchedDays.add(day.getId());
        return null;
    }

    private String applyRemove(int index, Remove r, TripEntity trip, List<UndoOp> undo, Set<Long> touchedDays) {
        Optional<ActivityEntity> found = activityInTrip(r.activityId(), trip.getId());
        if (found.isEmpty()) {
            return "ACTIVITY_ALREADY_DELETED";
        }
        ActivityEntity activity = found.get();
        if (isFinished(activity)) {
            return "ACTIVITY_FINISHED";
        }
        undo.add(new UndoOp.Removed(index, full(activity)));
        touchedDays.add(activity.getItineraryDay().getId());
        activityRepository.delete(activity);
        activityRepository.flush();
        return null;
    }

    private String applyUpdate(int index, Update u, TripEntity trip, Map<Integer, ItineraryDayEntity> days,
                               List<UndoOp> undo, Set<Long> touchedDays) {
        Optional<ActivityEntity> found = activityInTrip(u.activityId(), trip.getId());
        if (found.isEmpty()) {
            return "ACTIVITY_ALREADY_DELETED";
        }
        ActivityEntity activity = found.get();
        if (isFinished(activity)) {
            return "ACTIVITY_FINISHED";
        }
        FullSnapshot before = full(activity);
        touchedDays.add(activity.getItineraryDay().getId());
        if (u.dayNumber() != null) {
            ItineraryDayEntity target = days.get(u.dayNumber());
            if (target == null) {
                return "DAY_NOT_FOUND";
            }
            activity.setItineraryDay(target);
            activity.setOrderIndex(nextOrder(target.getId()));
            touchedDays.add(target.getId());
        }
        if (u.title() != null) {
            activity.setTitle(u.title());
        }
        LocalTime start = u.startTime() != null ? u.startTime() : activity.getStartTime();
        LocalTime end = u.endTime() != null ? u.endTime() : activity.getEndTime();
        if (start != null && end != null && end.isBefore(start)) {
            return "INVALID_TIME_RANGE";
        }
        activity.setStartTime(start);
        activity.setEndTime(end);
        if (u.estimatedCost() != null) {
            activity.setEstimatedCost(u.estimatedCost());
            activity.setEstimatedCostSource(CostSource.AI);
        }
        if (u.notes() != null) {
            activity.setNotes(u.notes());
        }
        activityRepository.saveAndFlush(activity);
        undo.add(new UndoOp.Updated(index, activity.getId(), before, null));
        return null;
    }

    private String applyReorder(int index, Reorder r, Map<Integer, ItineraryDayEntity> days, List<UndoOp> undo,
                                Set<Long> touchedDays) {
        ItineraryDayEntity day = days.get(r.dayNumber());
        List<Long> before = idsOfDay(day.getId());
        Map<Long, ActivityEntity> byId = new HashMap<>();
        activityRepository.findByItineraryDayIdOrderByOrderIndexAsc(day.getId()).forEach(a -> byId.put(a.getId(), a));
        for (int i = 0; i < r.activityIds().size(); i++) {
            byId.get(r.activityIds().get(i)).setOrderIndex((short) i);
        }
        activityRepository.saveAllAndFlush(byId.values());
        undo.add(new UndoOp.Reordered(index, day.getId(), r.dayNumber(), before, List.copyOf(r.activityIds())));
        touchedDays.add(day.getId());
        return null;
    }

    // ---------------------------------------------------------------- undo

    @Transactional
    public UndoProposalResponse undo(Long userId, Long tripId, Long proposalId, boolean preview) {
        AiProposalEntity proposal = (preview ? proposalRepository.findById(proposalId)
                : proposalRepository.findByIdForUpdate(proposalId))
                .filter(p -> p.getTripId().equals(tripId))
                .orElseThrow(() -> new AppException(ErrorCode.PROPOSAL_NOT_FOUND, "Proposal not found: " + proposalId));
        tripRepository.findByIdAndUserId(tripId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.PROPOSAL_NOT_FOUND, "Proposal not found: " + proposalId));
        if (proposal.getStatus() != ProposalStatus.APPLIED) {
            throw new AppException(ErrorCode.PROPOSAL_NOT_APPLIED, "Proposal is " + proposal.getStatus());
        }
        Instant closes = proposal.getAppliedAt().plus(properties.undoWindow());
        if (closes.isBefore(tripClock.now())) {
            throw new AppException(ErrorCode.UNDO_WINDOW_CLOSED, "Undo window closed at " + closes);
        }
        if (proposal.getUndo() == null) {
            throw new AppException(ErrorCode.UNDO_NOT_AVAILABLE);
        }
        UndoLog undoLog = objectMapper.convertValue(proposal.getUndo(), UndoLog.class);
        checkNotBlocked(proposal, undoLog);

        List<String> willDelete = new ArrayList<>();
        List<String> willRestore = new ArrayList<>();
        List<String> willRevert = new ArrayList<>();
        List<UndoProposalResponse.Skipped> skipped = new ArrayList<>();
        Set<Long> touchedDays = new HashSet<>();

        List<UndoOp> ops = new ArrayList<>(undoLog.ops());
        Collections.reverse(ops);
        for (UndoOp op : ops) {
            switch (op) {
                case UndoOp.Created c -> {
                    Optional<ActivityEntity> activity = activityRepository.findById(c.activityId());
                    if (activity.isEmpty()) {
                        skipped.add(new UndoProposalResponse.Skipped(c.title(), "ACTIVITY_ALREADY_DELETED"));
                    } else if (!fingerprint(activity.get()).equals(c.fingerprint())) {
                        skipped.add(new UndoProposalResponse.Skipped(c.title(), "ACTIVITY_EDITED_AFTER_APPLY"));
                    } else {
                        willDelete.add(c.title());
                        if (!preview) {
                            touchedDays.add(activity.get().getItineraryDay().getId());
                            activityRepository.delete(activity.get());
                        }
                    }
                }
                case UndoOp.Removed r -> {
                    Optional<ItineraryDayEntity> day = dayRepository.findById(r.snapshot().dayId());
                    if (day.isEmpty()) {
                        skipped.add(new UndoProposalResponse.Skipped(r.snapshot().title(), "DAY_NOT_FOUND"));
                    } else {
                        willRestore.add(r.snapshot().title());
                        if (!preview) {
                            restore(r.snapshot(), day.get());
                            touchedDays.add(day.get().getId());
                        }
                    }
                }
                case UndoOp.Updated u -> {
                    Optional<ActivityEntity> activity = activityRepository.findById(u.activityId());
                    String title = u.before().title();
                    if (activity.isEmpty()) {
                        skipped.add(new UndoProposalResponse.Skipped(title, "ACTIVITY_ALREADY_DELETED"));
                    } else if (!fingerprint(activity.get()).equals(u.fingerprint())) {
                        skipped.add(new UndoProposalResponse.Skipped(activity.get().getTitle(), "ACTIVITY_EDITED_AFTER_APPLY"));
                    } else {
                        willRevert.add(title);
                        if (!preview) {
                            touchedDays.add(activity.get().getItineraryDay().getId());
                            revertFields(activity.get(), u.before());
                            touchedDays.add(u.before().dayId());
                        }
                    }
                }
                case UndoOp.Reordered r -> {
                    String label = "Thứ tự ngày " + r.dayNumber();
                    if (!idsOfDay(r.dayId()).equals(r.afterOrder())) {
                        skipped.add(new UndoProposalResponse.Skipped(label, "ORDER_CHANGED_AFTER_APPLY"));
                    } else {
                        willRevert.add(label);
                        if (!preview) {
                            Map<Long, ActivityEntity> byId = new HashMap<>();
                            activityRepository.findByItineraryDayIdOrderByOrderIndexAsc(r.dayId())
                                    .forEach(a -> byId.put(a.getId(), a));
                            for (int i = 0; i < r.beforeOrder().size(); i++) {
                                byId.get(r.beforeOrder().get(i)).setOrderIndex((short) i);
                            }
                            activityRepository.saveAll(byId.values());
                        }
                    }
                }
            }
        }

        UndoProposalResponse.UndoProposalResponseBuilder response = UndoProposalResponse.builder()
                .proposalId(proposalId)
                .preview(preview)
                .reverted(willDelete.size() + willRestore.size() + willRevert.size())
                .willDelete(willDelete)
                .willRestore(willRestore)
                .willRevert(willRevert)
                .undoSkipped(skipped)
                .placesKept(undoLog.adoptedPlaces());
        if (preview) {
            return response.build();
        }
        activityRepository.flush();
        touchedDays.forEach(this::reindex);
        activityRepository.flush();
        proposal.setStatus(ProposalStatus.REVERTED);
        proposal.setRevertedAt(tripClock.now());
        proposalRepository.save(proposal);
        log.info("Reverted proposal ID={} on trip ID={}, {} skipped", proposalId, tripId, skipped.size());
        return response.itinerary(itineraryService.getItinerary(userId, tripId)).build();
    }

    /**
     * LIFO có nới (ADS-21 §4.5): được lùi nếu không có đề xuất nào áp dụng sau nó còn đụng
     * vào cùng hoạt động. Hai đề xuất ở hai ngày khác nhau lùi cái nào trước cũng được.
     */
    private void checkNotBlocked(AiProposalEntity proposal, UndoLog undoLog) {
        List<Long> blockedBy = new ArrayList<>();
        for (AiProposalEntity later : proposalRepository.findByTripIdAndStatusAndAppliedAtAfter(
                proposal.getTripId(), ProposalStatus.APPLIED, proposal.getAppliedAt())) {
            if (later.getUndo() == null) {
                continue;
            }
            UndoLog laterLog = objectMapper.convertValue(later.getUndo(), UndoLog.class);
            if (!Collections.disjoint(laterLog.touchedActivityIds(), undoLog.touchedActivityIds())) {
                blockedBy.add(later.getId());
            }
        }
        if (!blockedBy.isEmpty()) {
            throw new AppException(ErrorCode.UNDO_BLOCKED_BY_LATER,
                    "Undo the later proposals first: " + blockedBy, Map.of("blockedBy", blockedBy));
        }
    }

    private void restore(FullSnapshot s, ItineraryDayEntity day) {
        List<ActivityEntity> current = new ArrayList<>(activityRepository.findByItineraryDayIdOrderByOrderIndexAsc(day.getId()));
        ActivityEntity restored = ActivityEntity.builder()
                .itineraryDay(day)
                .title(s.title())
                .activityType(s.activityType())
                .place(s.placeId() == null ? null : placeRepository.findById(s.placeId()).orElse(null))
                .startTime(s.startTime())
                .endTime(s.endTime())
                .estimatedCost(s.estimatedCost())
                .estimatedCostSource(s.costSource())
                .transportationMode(s.transportationMode())
                .notes(s.notes())
                .status(s.status())
                .createdBy(s.createdBy())
                .fromProposalId(s.fromProposalId())
                .orderIndex((short) (current.size() + 1000))
                .build();
        current.add(Math.min(s.orderIndex(), current.size()), restored);
        for (int i = 0; i < current.size(); i++) {
            current.get(i).setOrderIndex((short) i);
        }
        activityRepository.saveAll(current);
    }

    private void revertFields(ActivityEntity a, FullSnapshot s) {
        if (a.getItineraryDay().getId() != s.dayId()) {
            dayRepository.findById(s.dayId()).ifPresent(day -> {
                a.setItineraryDay(day);
                a.setOrderIndex((short) (s.orderIndex() + 1000));
            });
        }
        a.setTitle(s.title());
        a.setStartTime(s.startTime());
        a.setEndTime(s.endTime());
        a.setEstimatedCost(s.estimatedCost());
        a.setEstimatedCostSource(s.costSource());
        a.setNotes(s.notes());
        activityRepository.save(a);
    }

    // ---------------------------------------------------------------- helpers

    private UndoOp withFingerprint(UndoOp op) {
        return switch (op) {
            case UndoOp.Created c -> new UndoOp.Created(c.changeIndex(), c.activityId(), c.title(),
                    fingerprint(activityRepository.findById(c.activityId()).orElseThrow()));
            case UndoOp.Updated u -> new UndoOp.Updated(u.changeIndex(), u.activityId(), u.before(),
                    fingerprint(activityRepository.findById(u.activityId()).orElseThrow()));
            case UndoOp.Removed r -> r;
            case UndoOp.Reordered r -> r;
        };
    }

    private Collection<Long> activityIdsOf(UndoOp op) {
        return switch (op) {
            case UndoOp.Created c -> List.of(c.activityId());
            case UndoOp.Updated u -> List.of(u.activityId());
            case UndoOp.Removed r -> List.of();
            case UndoOp.Reordered r -> r.afterOrder();
        };
    }

    /**
     * Dấu vân tay nội dung của một hoạt động (không gồm thứ tự). Người dùng sửa bất kỳ trường
     * nào sau khi áp dụng thì dấu vân tay đổi, và hoàn tác giữ nguyên hoạt động đó.
     */
    static String fingerprint(ActivityEntity a) {
        return String.join("|", String.valueOf(a.getItineraryDay().getId()), a.getTitle(),
                String.valueOf(a.getActivityType()), String.valueOf(a.getPlace() == null ? null : a.getPlace().getId()),
                String.valueOf(a.getStartTime()), String.valueOf(a.getEndTime()), String.valueOf(a.getEstimatedCost()),
                String.valueOf(a.getNotes()), String.valueOf(a.getTransportationMode()), String.valueOf(a.getStatus()));
    }

    private FullSnapshot full(ActivityEntity a) {
        return new FullSnapshot(a.getItineraryDay().getId(), a.getOrderIndex(), a.getTitle(), a.getActivityType(),
                a.getPlace() == null ? null : a.getPlace().getId(), a.getStartTime(), a.getEndTime(), a.getEstimatedCost(),
                a.getEstimatedCostSource(), a.getTransportationMode(), a.getNotes(), a.getStatus(), a.getCreatedBy(),
                a.getFromProposalId());
    }

    private ApplyProposalResponse.Skipped skipped(ProposalChange change, String reason) {
        return switch (change) {
            case Add a -> new ApplyProposalResponse.Skipped("ADD", null, a.title(), reason);
            case Remove r -> new ApplyProposalResponse.Skipped("REMOVE", r.activityId(), r.before().title(), reason);
            case Update u -> new ApplyProposalResponse.Skipped("UPDATE", u.activityId(), u.before().title(), reason);
            case Reorder r -> new ApplyProposalResponse.Skipped("REORDER", null, "Ngày " + r.dayNumber(), reason);
        };
    }

    private boolean isFinished(ActivityEntity a) {
        return a.getStatus() == ActivityStatus.DONE || a.getStatus() == ActivityStatus.SKIPPED;
    }

    private Optional<ActivityEntity> activityInTrip(long activityId, long tripId) {
        return activityRepository.findById(activityId).filter(a -> a.getItineraryDay().getTrip().getId() == tripId);
    }

    private Map<Integer, ItineraryDayEntity> daysByNumber(Long tripId) {
        Map<Integer, ItineraryDayEntity> days = new HashMap<>();
        dayRepository.findByTripIdOrderByDayNumberAsc(tripId).forEach(d -> days.put((int) d.getDayNumber(), d));
        return days;
    }

    private List<Long> idsOfDay(Long dayId) {
        return activityRepository.findByItineraryDayIdOrderByOrderIndexAsc(dayId).stream().map(ActivityEntity::getId).toList();
    }

    private short nextOrder(Long dayId) {
        Short max = activityRepository.findMaxOrderIndexByItineraryDayId(dayId);
        return (short) (max == null ? 0 : max + 1);
    }

    /** Đánh lại {@code order_index} liên tục từ 0 (ràng buộc UNIQUE được hoãn tới lúc commit). */
    private void reindex(Long dayId) {
        List<ActivityEntity> activities = activityRepository.findByItineraryDayIdOrderByOrderIndexAsc(dayId);
        for (int i = 0; i < activities.size(); i++) {
            activities.get(i).setOrderIndex((short) i);
        }
        activityRepository.saveAll(activities);
    }
}
