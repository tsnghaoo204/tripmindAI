package com.tripmind.repositories;

import com.tripmind.entities.MessageEntity;
import com.tripmind.enums.MessageRole;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    List<MessageEntity> findByConversationIdOrderByCreatedAtAscIdAsc(Long conversationId);

    /** Các tin nhắn gần nhất (mới trước) để dựng lại ngữ cảnh. */
    List<MessageEntity> findByConversationIdAndRoleInOrderByIdDesc(Long conversationId, Collection<MessageRole> roles,
                                                                   Pageable pageable);
}
