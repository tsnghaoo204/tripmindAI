package com.tripmind.repositories;

import com.tripmind.entities.ActivityAlternativeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ActivityAlternativeRepository extends JpaRepository<ActivityAlternativeEntity, Long> {

    @Query("SELECT a FROM ActivityAlternativeEntity a LEFT JOIN FETCH a.place WHERE a.activity.id = :activityId ORDER BY a.id ASC")
    List<ActivityAlternativeEntity> findByActivityIdWithPlace(@Param("activityId") Long activityId);

    List<ActivityAlternativeEntity> findByActivityIdOrderByIdAsc(Long activityId);

    @Query("SELECT a FROM ActivityAlternativeEntity a LEFT JOIN FETCH a.place WHERE a.activity.id IN :activityIds ORDER BY a.id ASC")
    List<ActivityAlternativeEntity> findByActivityIdsWithPlace(@Param("activityIds") List<Long> activityIds);

    Optional<ActivityAlternativeEntity> findByIdAndActivityId(Long id, Long activityId);

    void deleteByActivityId(Long activityId);
}
