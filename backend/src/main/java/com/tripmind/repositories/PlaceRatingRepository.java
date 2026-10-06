package com.tripmind.repositories;

import com.tripmind.entities.PlaceRatingEntity;
import com.tripmind.enums.PlaceVerdict;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaceRatingRepository extends JpaRepository<PlaceRatingEntity, Long> {

    Optional<PlaceRatingEntity> findByUserIdAndPlaceId(Long userId, Long placeId);

    List<PlaceRatingEntity> findByUserIdOrderByUpdatedAtDesc(Long userId);

    List<PlaceRatingEntity> findByUserIdAndVerdictOrderByUpdatedAtDesc(Long userId, PlaceVerdict verdict, Pageable pageable);

    List<PlaceRatingEntity> findByUserIdAndVerdict(Long userId, PlaceVerdict verdict);
}
