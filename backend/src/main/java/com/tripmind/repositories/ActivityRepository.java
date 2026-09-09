package com.tripmind.repositories;

import com.tripmind.entities.ActivityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityRepository extends JpaRepository<ActivityEntity, Long> {

    List<ActivityEntity> findByItineraryDayIdOrderByOrderIndexAsc(Long itineraryDayId);

    List<ActivityEntity> findByItineraryDayTripIdOrderByItineraryDayDayNumberAscOrderIndexAsc(Long tripId);

    @Query("SELECT MAX(a.orderIndex) FROM ActivityEntity a WHERE a.itineraryDay.id = :dayId")
    Short findMaxOrderIndexByItineraryDayId(@Param("dayId") Long dayId);

    @Query("SELECT a FROM ActivityEntity a WHERE a.id = :id AND a.itineraryDay.trip.user.id = :userId")
    Optional<ActivityEntity> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
