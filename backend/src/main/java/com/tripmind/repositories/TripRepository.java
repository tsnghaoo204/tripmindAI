package com.tripmind.repositories;

import com.tripmind.entities.TripEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<TripEntity, Long> {

    List<TripEntity> findByUserIdOrderByStartDateDesc(Long userId);

    Optional<TripEntity> findByIdAndUserId(Long id, Long userId);

    /**
     * Số ngày "đã lên lịch" của từng chuyến: ngày có ít nhất {@code minActivities} hoạt động.
     * Trả {@code [tripId, count]}; chuyến không có ngày nào đủ thì không có dòng.
     */
    @Query("""
            SELECT d.trip.id, COUNT(d) FROM ItineraryDayEntity d
            WHERE d.trip.id IN :tripIds
              AND (SELECT COUNT(a) FROM ActivityEntity a WHERE a.itineraryDay = d) >= :minActivities
            GROUP BY d.trip.id
            """)
    List<Object[]> countPlannedDays(@Param("tripIds") List<Long> tripIds, @Param("minActivities") long minActivities);
}
