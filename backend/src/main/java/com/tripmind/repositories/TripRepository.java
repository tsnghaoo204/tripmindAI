package com.tripmind.repositories;

import com.tripmind.entities.TripEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<TripEntity, Long> {

    List<TripEntity> findByUserIdOrderByStartDateDesc(Long userId);

    Optional<TripEntity> findByIdAndUserId(Long id, Long userId);
}
