package com.tripmind.repositories;

import com.tripmind.entities.BlogCommentUpvoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface BlogCommentUpvoteRepository extends JpaRepository<BlogCommentUpvoteEntity, Long> {

    Optional<BlogCommentUpvoteEntity> findByUserIdAndCommentId(Long userId, Long commentId);

    boolean existsByUserIdAndCommentId(Long userId, Long commentId);

    void deleteByUserIdAndCommentId(Long userId, Long commentId);

    @Query("SELECT u.commentId FROM BlogCommentUpvoteEntity u WHERE u.userId = :userId AND u.commentId IN :commentIds")
    Set<Long> findUpvotedCommentIds(@Param("userId") Long userId, @Param("commentIds") Iterable<Long> commentIds);
}
