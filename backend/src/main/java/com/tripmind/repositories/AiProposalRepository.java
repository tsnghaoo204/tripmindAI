package com.tripmind.repositories;

import com.tripmind.entities.AiProposalEntity;
import com.tripmind.enums.ProposalStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AiProposalRepository extends JpaRepository<AiProposalEntity, Long> {

    /** Khoá dòng đề xuất: hai lượt bấm "áp dụng" cùng lúc thì lượt sau chờ và thấy APPLIED (BR-508). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM AiProposalEntity p WHERE p.id = :id")
    Optional<AiProposalEntity> findByIdForUpdate(@Param("id") Long id);

    List<AiProposalEntity> findByTripIdAndStatusAndAppliedAtAfter(Long tripId, ProposalStatus status, Instant appliedAt);

    @Query("SELECT COALESCE(MAX(p.appliedSeq), 0) FROM AiProposalEntity p WHERE p.tripId = :tripId")
    long maxAppliedSeq(@Param("tripId") Long tripId);

    @Modifying
    @Query("UPDATE AiProposalEntity p SET p.status = com.tripmind.enums.ProposalStatus.EXPIRED "
            + "WHERE p.status = com.tripmind.enums.ProposalStatus.PENDING AND p.expiresAt < :now")
    int expirePending(@Param("now") Instant now);
}
