package com.tripmind.services.ai.tools;

import com.tripmind.entities.PlaceRatingEntity;
import com.tripmind.enums.PlaceVerdict;
import com.tripmind.repositories.PlaceRatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Địa điểm người dùng đã thích / đã chê ở các chuyến trước. Công cụ tìm kiếm và bước sinh
 * lịch trình lọc bằng mã theo mã ngoài của địa điểm, không trông vào việc mô hình tự nhớ.
 */
@Component
@RequiredArgsConstructor
public class PlaceRatingLookup {

    private static final int NAMES_FOR_PROMPT = 10;

    private final PlaceRatingRepository repository;

    @Transactional(readOnly = true)
    public Set<String> dislikedExternalIds(Long userId) {
        return externalIds(userId, PlaceVerdict.DISLIKE);
    }

    @Transactional(readOnly = true)
    public Set<String> likedExternalIds(Long userId) {
        return externalIds(userId, PlaceVerdict.LIKE);
    }

    @Transactional(readOnly = true)
    public List<String> likedNames(Long userId) {
        return names(userId, PlaceVerdict.LIKE);
    }

    @Transactional(readOnly = true)
    public List<String> dislikedNames(Long userId) {
        return names(userId, PlaceVerdict.DISLIKE);
    }

    private Set<String> externalIds(Long userId, PlaceVerdict verdict) {
        return repository.findByUserIdAndVerdict(userId, verdict).stream()
                .map(r -> r.getPlace().getExternalId())
                .collect(Collectors.toSet());
    }

    private List<String> names(Long userId, PlaceVerdict verdict) {
        return repository.findByUserIdAndVerdictOrderByUpdatedAtDesc(userId, verdict, PageRequest.of(0, NAMES_FOR_PROMPT))
                .stream()
                .map(PlaceRatingEntity::getPlace)
                .map(p -> p.getName())
                .toList();
    }
}
