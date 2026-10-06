package com.tripmind.services;

import com.tripmind.domains.requests.UserPreferencesRequest;
import com.tripmind.domains.responses.UserPreferencesResponse;
import com.tripmind.entities.UserEntity;
import com.tripmind.entities.UserPreferencesEntity;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import com.tripmind.repositories.UserPreferencesRepository;
import com.tripmind.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Sở thích mặc định của tài khoản (FR-007). Chuyến đi mới lấy giá trị từ đây khi người dùng bỏ trống. */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserPreferencesService {

    private final UserPreferencesRepository userPreferencesRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserPreferencesResponse get(Long userId) {
        return userPreferencesRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseGet(() -> UserPreferencesResponse.builder().preferences(List.of()).build());
    }

    @Transactional
    public UserPreferencesResponse update(Long userId, UserPreferencesRequest request) {
        UserPreferencesEntity entity = userPreferencesRepository.findByUserId(userId).orElseGet(() -> {
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User not found: " + userId));
            return UserPreferencesEntity.builder().user(user).build();
        });
        entity.setTravelStyle(request.getTravelStyle());
        entity.setBudgetPreference(request.getBudgetPreference());
        entity.setPreferences(request.getPreferences() == null ? new ArrayList<>() : new ArrayList<>(request.getPreferences()
                .stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new))));
        UserPreferencesEntity saved = userPreferencesRepository.save(entity);
        log.info("Updated default preferences for user ID={}", userId);
        return toResponse(saved);
    }

    private UserPreferencesResponse toResponse(UserPreferencesEntity entity) {
        return UserPreferencesResponse.builder()
                .travelStyle(entity.getTravelStyle())
                .budgetPreference(entity.getBudgetPreference())
                .preferences(entity.getPreferences())
                .build();
    }
}
