package com.tripmind.repositories;

import com.tripmind.entities.ItineraryDayEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItineraryDayRepository extends JpaRepository<ItineraryDayEntity, Long> {

    List<ItineraryDayEntity> findByTripIdOrderByDayNumberAsc(Long tripId);

    Optional<ItineraryDayEntity> findByTripIdAndDayNumber(Long tripId, short dayNumber);

    @Query("SELECT d FROM ItineraryDayEntity d WHERE d.id = :id AND d.trip.user.id = :userId")
    Optional<ItineraryDayEntity> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
