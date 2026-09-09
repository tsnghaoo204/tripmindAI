package com.tripmind.repositories;

import com.tripmind.entities.SavedPlaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedPlaceRepository extends JpaRepository<SavedPlaceEntity, Long> {

    List<SavedPlaceEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<SavedPlaceEntity> findByUserIdAndPlaceId(Long userId, Long placeId);

    boolean existsByUserIdAndPlaceId(Long userId, Long placeId);

    void deleteByUserIdAndPlaceId(Long userId, Long placeId);
}
