package com.tripmind.services;

import com.tripmind.domains.requests.ChecklistItemRequest;
import com.tripmind.domains.requests.UpdateChecklistItemRequest;
import com.tripmind.domains.responses.ChecklistItemResponse;
import com.tripmind.domains.responses.ChecklistSuggestionsResponse;
import com.tripmind.entities.ChecklistItemEntity;
import com.tripmind.entities.TripEntity;
import com.tripmind.enums.ChecklistKind;
import com.tripmind.enums.ChecklistSource;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import com.tripmind.repositories.ActivityRepository;
import com.tripmind.repositories.ChecklistItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** Checklist chuẩn bị chuyến đi: đồ mang theo và việc cần làm trước chuyến. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChecklistService {

    private final ChecklistItemRepository repository;
    private final ActivityRepository activityRepository;
    private final TripService tripService;
    private final WeatherService weatherService;
    private final ChecklistSuggester suggester;

    @Transactional(readOnly = true)
    public List<ChecklistItemResponse> list(Long userId, Long tripId, ChecklistKind kind) {
        tripService.getOwnedTrip(userId, tripId);
        List<ChecklistItemEntity> items = kind == null
                ? repository.findByTripIdOrderByKindAscOrderIndexAscIdAsc(tripId)
                : repository.findByTripIdAndKindOrderByOrderIndexAscIdAsc(tripId, kind);
        return items.stream().map(this::toResponse).toList();
    }

    /** Thêm một hoặc nhiều mục; mục đã có (trùng tên, không phân biệt hoa thường) thì bỏ qua. */
    @Transactional
    public List<ChecklistItemResponse> add(Long userId, Long tripId, List<ChecklistItemRequest> requests) {
        TripEntity trip = tripService.getOwnedTrip(userId, tripId);
        Set<String> existing = new HashSet<>();
        repository.findByTripIdOrderByKindAscOrderIndexAscIdAsc(tripId).forEach(i -> existing.add(key(i.getKind(), i.getTitle())));
        Map<ChecklistKind, Short> nextOrder = new EnumMap<>(ChecklistKind.class);
        List<ChecklistItemResponse> created = new ArrayList<>();
        for (ChecklistItemRequest request : requests) {
            String title = request.getTitle().strip();
            if (!existing.add(key(request.getKind(), title))) {
                continue;
            }
            short order = nextOrder.computeIfAbsent(request.getKind(), k -> repository.maxOrderIndex(tripId, k));
            order++;
            nextOrder.put(request.getKind(), order);
            ChecklistItemEntity saved = repository.save(ChecklistItemEntity.builder()
                    .trip(trip)
                    .kind(request.getKind())
                    .title(title)
                    .category(request.getCategory())
                    .dueDate(request.getKind() == ChecklistKind.TODO ? request.getDueDate() : null)
                    .source(request.getSource() == null ? ChecklistSource.USER : request.getSource())
                    .reason(request.getReason())
                    .orderIndex(order)
                    .build());
            created.add(toResponse(saved));
        }
        log.info("Added {} checklist items to trip ID={}", created.size(), tripId);
        return created;
    }

    @Transactional
    public ChecklistItemResponse update(Long userId, Long itemId, UpdateChecklistItemRequest request) {
        ChecklistItemEntity item = owned(userId, itemId);
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            item.setTitle(request.getTitle().strip());
        }
        if (request.getDone() != null) {
            item.setDone(request.getDone());
        }
        if (request.getCategory() != null) {
            item.setCategory(request.getCategory());
        }
        if (request.getDueDate() != null && item.getKind() == ChecklistKind.TODO) {
            item.setDueDate(request.getDueDate());
        }
        return toResponse(repository.save(item));
    }

    @Transactional
    public void delete(Long userId, Long itemId) {
        repository.delete(owned(userId, itemId));
    }

    /**
     * Ứng viên từ bộ luật, đã bỏ những mục chuyến đã có. Không bọc giao dịch để không giữ
     * kết nối CSDL trong lúc chờ dịch vụ thời tiết.
     */
    public ChecklistSuggestionsResponse suggestions(Long userId, Long tripId) {
        TripEntity trip = tripService.getOwnedTrip(userId, tripId);
        Set<String> existing = new HashSet<>();
        repository.findByTripIdOrderByKindAscOrderIndexAscIdAsc(tripId).forEach(i -> existing.add(key(i.getKind(), i.getTitle())));
        ChecklistSuggestionsResponse response = suggester.suggest(trip, activityRepository.findWithPlacesByTripId(tripId),
                weatherService.forTrip(trip));
        response.setItems(response.getItems().stream()
                .filter(i -> !existing.contains(key(i.getKind(), i.getTitle())))
                .toList());
        return response;
    }

    private ChecklistItemEntity owned(Long userId, Long itemId) {
        return repository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Checklist item not found: " + itemId));
    }

    private static String key(ChecklistKind kind, String title) {
        return kind + ":" + title.strip().toLowerCase(Locale.ROOT);
    }

    private ChecklistItemResponse toResponse(ChecklistItemEntity e) {
        return ChecklistItemResponse.builder()
                .id(e.getId())
                .kind(e.getKind())
                .title(e.getTitle())
                .category(e.getCategory())
                .dueDate(e.getDueDate())
                .done(e.isDone())
                .source(e.getSource())
                .reason(e.getReason())
                .build();
    }
}
