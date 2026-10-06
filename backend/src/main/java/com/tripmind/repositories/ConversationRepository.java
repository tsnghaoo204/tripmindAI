package com.tripmind.repositories;

import com.tripmind.entities.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {

    List<ConversationEntity> findByTripIdAndUserIdOrderByUpdatedAtDesc(Long tripId, Long userId);

    Optional<ConversationEntity> findByIdAndUserId(Long id, Long userId);
}
