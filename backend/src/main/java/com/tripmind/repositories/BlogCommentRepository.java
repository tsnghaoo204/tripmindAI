package com.tripmind.repositories;

import com.tripmind.entities.BlogCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BlogCommentRepository extends JpaRepository<BlogCommentEntity, Long> {

    List<BlogCommentEntity> findByPostIdOrderByCreatedAtAsc(Long postId);

    @Modifying
    @Query("UPDATE BlogCommentEntity c SET c.upvoteCount = GREATEST(0, c.upvoteCount + :delta) WHERE c.id = :commentId")
    void updateUpvoteCount(@Param("commentId") Long commentId, @Param("delta") int delta);
}
