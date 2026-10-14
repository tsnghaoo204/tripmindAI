package com.tripmind.repositories;

import com.tripmind.entities.BlogCommentUpvoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface BlogCommentUpvoteRepository extends JpaRepository<BlogCommentUpvoteEntity, Long> {

    Optional<BlogCommentUpvoteEntity> findByUserIdAndCommentId(Long userId, Long commentId);

    boolean existsByUserIdAndCommentId(Long userId, Long commentId);

    /** Trả 1 khi vừa thêm, 0 khi đã có sẵn. */
    @Modifying
    @Query(value = "INSERT INTO blog_comment_upvotes (user_id, comment_id) VALUES (:userId, :commentId) "
            + "ON CONFLICT (user_id, comment_id) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("userId") Long userId, @Param("commentId") Long commentId);

    @Modifying
    @Query("DELETE FROM BlogCommentUpvoteEntity u WHERE u.userId = :userId AND u.commentId = :commentId")
    int deleteVote(@Param("userId") Long userId, @Param("commentId") Long commentId);

    long countByCommentId(Long commentId);

    @Query("SELECT u.commentId FROM BlogCommentUpvoteEntity u WHERE u.userId = :userId AND u.commentId IN :commentIds")
    Set<Long> findUpvotedCommentIds(@Param("userId") Long userId, @Param("commentIds") Iterable<Long> commentIds);
}
