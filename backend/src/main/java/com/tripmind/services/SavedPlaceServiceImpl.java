package com.tripmind.services;

import com.tripmind.domains.responses.PlaceResponse;
import com.tripmind.domains.responses.SavedPlaceResponse;
import com.tripmind.entities.PlaceEntity;
import com.tripmind.entities.SavedPlaceEntity;
import com.tripmind.entities.UserEntity;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import com.tripmind.repositories.PlaceRepository;
import com.tripmind.repositories.SavedPlaceRepository;
import com.tripmind.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavedPlaceServiceImpl implements SavedPlaceService {

    private final SavedPlaceRepository savedPlaceRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public SavedPlaceResponse savePlace(Long userId, Long placeId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User not found with id: " + userId));

        PlaceEntity place = placeRepository.findById(placeId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Place not found with id: " + placeId));

        Optional<SavedPlaceEntity> existing = savedPlaceRepository.findByUserIdAndPlaceId(userId, placeId);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        SavedPlaceEntity entity = SavedPlaceEntity.builder()
                .user(user)
                .place(place)
                .build();

        SavedPlaceEntity saved = savedPlaceRepository.save(entity);
        log.info("User ID={} saved place ID={} ({})", userId, placeId, place.getName());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void unsavePlace(Long userId, Long placeId) {
        savedPlaceRepository.deleteByUserIdAndPlaceId(userId, placeId);
        log.info("User ID={} unsaved place ID={}", userId, placeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedPlaceResponse> getSavedPlaces(Long userId) {
        return savedPlaceRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPlaceSaved(Long userId, Long placeId) {
        return savedPlaceRepository.existsByUserIdAndPlaceId(userId, placeId);
    }

    private SavedPlaceResponse toResponse(SavedPlaceEntity entity) {
        return SavedPlaceResponse.builder()
                .id(entity.getId())
                .place(PlaceResponse.fromEntity(entity.getPlace()))
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
