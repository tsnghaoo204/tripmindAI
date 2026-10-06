package com.tripmind.repositories;

import com.tripmind.entities.AiToolExecutionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AiToolExecutionRepository extends JpaRepository<AiToolExecutionEntity, Long>,
        JpaSpecificationExecutor<AiToolExecutionEntity> {

    Page<AiToolExecutionEntity> findByTripIdAndUserIdOrderByIdDesc(Long tripId, Long userId, Pageable pageable);

    List<AiToolExecutionEntity> findByIdInOrderByIdAsc(Collection<Long> ids);

    @Modifying
    @Query("UPDATE AiToolExecutionEntity e SET e.messageId = :messageId WHERE e.id IN :ids")
    int linkToMessage(@Param("ids") Collection<Long> ids, @Param("messageId") Long messageId);
}
