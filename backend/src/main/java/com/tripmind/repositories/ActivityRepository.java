package com.tripmind.repositories;

import com.tripmind.entities.ActivityEntity;
import com.tripmind.enums.ActivityStatus;
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

    Optional<ActivityEntity> findFirstByItineraryDayTripIdAndStatusAndIdNot(Long tripId, ActivityStatus status, Long id);

    long countByItineraryDayTripId(Long tripId);

    /**
     * {@code [activityType, sum]} chi phí ước tính của một chuyến, bỏ hoạt động đã bỏ qua
     * (FR-1105): thứ người dùng không làm thì không tính là sẽ tiêu.
     */
    @Query("""
            SELECT a.activityType, SUM(a.estimatedCost) FROM ActivityEntity a
            WHERE a.itineraryDay.trip.id = :tripId AND a.estimatedCost IS NOT NULL
              AND a.status <> com.tripmind.enums.ActivityStatus.SKIPPED
            GROUP BY a.activityType
            """)
    List<Object[]> sumEstimatedByType(@Param("tripId") Long tripId);
}
