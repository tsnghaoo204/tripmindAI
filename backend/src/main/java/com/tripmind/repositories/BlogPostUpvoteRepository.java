package com.tripmind.repositories;

import com.tripmind.entities.BlogPostUpvoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlogPostUpvoteRepository extends JpaRepository<BlogPostUpvoteEntity, Long> {

    Optional<BlogPostUpvoteEntity> findByUserIdAndPostId(Long userId, Long postId);

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    void deleteByUserIdAndPostId(Long userId, Long postId);

    @Query("SELECT u.postId FROM BlogPostUpvoteEntity u WHERE u.userId = :userId AND u.postId IN :postIds")
    Set<Long> findUpvotedPostIds(@Param("userId") Long userId, @Param("postIds") Iterable<Long> postIds);
}
