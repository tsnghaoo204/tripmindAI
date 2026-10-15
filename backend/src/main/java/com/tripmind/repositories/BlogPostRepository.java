package com.tripmind.repositories;

import com.tripmind.entities.BlogPostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BlogPostRepository extends JpaRepository<BlogPostEntity, Long> {

    @Query("SELECT p FROM BlogPostEntity p " +
           "WHERE (:destination IS NULL OR :destination = '' OR LOWER(p.destinationName) = LOWER(:destination)) " +
           "  AND (:search IS NULL OR :search = '' OR " +
           "       LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "       LOWER(p.content) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY p.createdAt DESC")
    Page<BlogPostEntity> findFiltered(@Param("destination") String destination,
                                      @Param("search") String search,
                                      Pageable pageable);

    @Query("SELECT DISTINCT p.destinationName FROM BlogPostEntity p ORDER BY p.destinationName ASC")
    List<String> findDistinctDestinations();

    /** Đếm lại từ bảng upvote rồi ghi vào bài; luôn khớp dù hai lượt bấm chạy song song. */
    @Modifying
    @Query(value = "UPDATE blog_posts SET upvote_count = (SELECT COUNT(*) FROM blog_post_upvotes WHERE post_id = :postId) "
            + "WHERE id = :postId", nativeQuery = true)
    int syncUpvoteCount(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE BlogPostEntity p SET p.commentCount = GREATEST(0, p.commentCount + :delta) WHERE p.id = :postId")
    void updateCommentCount(@Param("postId") Long postId, @Param("delta") int delta);

    boolean existsByTripId(Long tripId);
}
