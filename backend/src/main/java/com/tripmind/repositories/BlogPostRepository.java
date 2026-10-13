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

    @Modifying
    @Query("UPDATE BlogPostEntity p SET p.upvoteCount = GREATEST(0, p.upvoteCount + :delta) WHERE p.id = :postId")
    void updateUpvoteCount(@Param("postId") Long postId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE BlogPostEntity p SET p.commentCount = GREATEST(0, p.commentCount + :delta) WHERE p.id = :postId")
    void updateCommentCount(@Param("postId") Long postId, @Param("delta") int delta);
}
