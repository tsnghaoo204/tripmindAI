package com.tripmind.repositories;

import com.tripmind.entities.BlogCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BlogCommentRepository extends JpaRepository<BlogCommentEntity, Long> {

    List<BlogCommentEntity> findByPostIdOrderByCreatedAtAsc(Long postId);

    /** Đếm lại từ bảng upvote rồi ghi vào bình luận. */
    @Modifying
    @Query(value = "UPDATE blog_comments SET upvote_count = (SELECT COUNT(*) FROM blog_comment_upvotes WHERE comment_id = :commentId) "
            + "WHERE id = :commentId", nativeQuery = true)
    int syncUpvoteCount(@Param("commentId") Long commentId);
}
