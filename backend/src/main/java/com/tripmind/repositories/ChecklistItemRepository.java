package com.tripmind.repositories;

import com.tripmind.entities.ChecklistItemEntity;
import com.tripmind.enums.ChecklistKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChecklistItemRepository extends JpaRepository<ChecklistItemEntity, Long> {

    List<ChecklistItemEntity> findByTripIdOrderByKindAscOrderIndexAscIdAsc(Long tripId);

    List<ChecklistItemEntity> findByTripIdAndKindOrderByOrderIndexAscIdAsc(Long tripId, ChecklistKind kind);

    @Query("SELECT c FROM ChecklistItemEntity c WHERE c.id = :id AND c.trip.user.id = :userId")
    Optional<ChecklistItemEntity> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT COALESCE(MAX(c.orderIndex), -1) FROM ChecklistItemEntity c WHERE c.trip.id = :tripId AND c.kind = :kind")
    short maxOrderIndex(@Param("tripId") Long tripId, @Param("kind") ChecklistKind kind);
}
