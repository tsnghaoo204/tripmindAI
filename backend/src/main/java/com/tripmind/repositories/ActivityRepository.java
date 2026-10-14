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

    /** {@code [dayId, count]} số hoạt động từng ngày của chuyến; ngày trống không có dòng. */
    @Query("SELECT a.itineraryDay.id, COUNT(a) FROM ActivityEntity a WHERE a.itineraryDay.trip.id = :tripId GROUP BY a.itineraryDay.id")
    List<Object[]> countByDay(@Param("tripId") Long tripId);

    /** Có chi tiêu, nhật ký hoặc ảnh gắn với hoạt động: xoá nó sẽ mất (hoặc tách rời) dữ liệu người dùng. */
    @Query(value = """
            SELECT EXISTS (SELECT 1 FROM expenses WHERE activity_id = :id)
                OR EXISTS (SELECT 1 FROM journal_entries WHERE activity_id = :id)
                OR EXISTS (SELECT 1 FROM photos WHERE activity_id = :id)
            """, nativeQuery = true)
    boolean hasLinkedData(@Param("id") Long id);

    @Query("SELECT COUNT(a) > 0 FROM ActivityEntity a WHERE a.itineraryDay.trip.id = :tripId AND a.estimatedCost > 0")
    boolean existsPricedByTripId(@Param("tripId") Long tripId);

    /** Một hoạt động kèm ngày, chuyến, điểm đến và địa điểm — dùng được ngoài giao dịch. */
    @Query("""
            SELECT a FROM ActivityEntity a JOIN FETCH a.itineraryDay d JOIN FETCH d.trip t
            LEFT JOIN FETCH t.destination LEFT JOIN FETCH a.place WHERE a.id = :id
            """)
    Optional<ActivityEntity> findDetachedById(@Param("id") Long id);

    /** Mọi hoạt động của chuyến kèm địa điểm và ngày, đọc một lần — dùng được ngoài giao dịch. */
    @Query("""
            SELECT a FROM ActivityEntity a JOIN FETCH a.itineraryDay d LEFT JOIN FETCH a.place
            WHERE d.trip.id = :tripId ORDER BY d.dayNumber, a.orderIndex
            """)
    List<ActivityEntity> findWithPlacesByTripId(@Param("tripId") Long tripId);

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
